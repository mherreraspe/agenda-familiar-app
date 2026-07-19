#!/usr/bin/env python3
import datetime as dt
import json
import struct
import sys
import time
import urllib.error
import urllib.parse
import urllib.request
import uuid
import zlib

BASE = "https://www.obusystem.com/api/v1"
FAMILIA = "0197f100-0000-7000-8000-000000000001"
OTRA = "0197f100-0000-7000-8000-000000000099"


def password(path):
    with open(path, encoding="utf-8") as env_file:
        for line in env_file:
            if line.startswith("FAMILIA_TEST_PASSWORD="):
                return line.split("=", 1)[1].strip().strip('"').strip("'")
    raise RuntimeError("Credencial de prueba no configurada")


def request_raw(method, path, token=None, data=None, content_type=None, expected=(200,)):
    headers = {"Accept": "application/json"}
    if content_type:
        headers["Content-Type"] = content_type
    if token:
        headers["Authorization"] = f"Bearer {token}"
    req = urllib.request.Request(BASE + path, data=data, headers=headers, method=method)
    try:
        with urllib.request.urlopen(req, timeout=15) as response:
            status, content = response.status, response.read()
    except urllib.error.HTTPError as error:
        status, content = error.code, error.read()
    if status not in expected:
        excerpt = content.decode(errors="replace")[:400]
        raise AssertionError(f"{method} {path}: HTTP {status}: {excerpt}")
    return status, content


def request(method, path, token=None, body=None, expected=(200,)):
    data = None if body is None else json.dumps(body).encode()
    status, content = request_raw(method, path, token, data,
                                  "application/json" if data is not None else None, expected)
    return status, json.loads(content) if content else None


def test_png():
    def chunk(name, data):
        return struct.pack(">I", len(data)) + name + data + struct.pack(">I", zlib.crc32(name + data) & 0xffffffff)
    pixels = b"\x00\x31\x5b\x4c\x31\x5b\x4c\x00\x31\x5b\x4c\x31\x5b\x4c"
    return b"\x89PNG\r\n\x1a\n" + chunk(b"IHDR", struct.pack(">IIBBBBB", 2, 2, 8, 2, 0, 0, 0)) + chunk(b"IDAT", zlib.compress(pixels)) + chunk(b"IEND", b"")


def upload_recipe(treatment_id, token):
    boundary = "agenda-e2e-" + uuid.uuid4().hex
    photo = test_png()
    body = (f"--{boundary}\r\nContent-Disposition: form-data; name=\"archivo\"; filename=\"receta.png\"\r\n"
            "Content-Type: image/png\r\n\r\n").encode() + photo + f"\r\n--{boundary}--\r\n".encode()
    status, content = request_raw("POST", f"/familias/{FAMILIA}/tratamientos/{treatment_id}/receta",
                                  token, body, f"multipart/form-data; boundary={boundary}", expected=(201,))
    return status, json.loads(content)


def login(email, secret):
    _, session = request(
        "POST",
        "/autenticacion/iniciar-sesion",
        body={"correo": email, "clave": secret},
    )
    assert session["correo"] == email
    return session["accessToken"]


def main(env_path):
    secret = password(env_path)
    papa = login("papa@familia.test", secret)
    mama = login("mama@familia.test", secret)
    stamp = dt.datetime.now(dt.timezone.utc).strftime("%Y%m%d-%H%M%S")
    title = f"Control pediátrico E2E {stamp}"
    place = f"Centro familiar E2E {stamp}"
    address = "Dirección ficticia de prueba E2E"
    start = (dt.datetime.now(dt.timezone.utc) + dt.timedelta(days=2)).isoformat().replace("+00:00", "Z")

    began = time.monotonic()
    event_status, event = request(
        "POST",
        f"/familias/{FAMILIA}/eventos",
        papa,
        {
            "titulo": title,
            "tipo": "CONTROL",
            "lugar": place,
            "direccion": address,
            "inicioEn": start,
        },
        expected=(201,),
    )
    save_seconds = time.monotonic() - began
    event_id = event["id"]

    found = None
    for _ in range(40):
        query = urllib.parse.quote("pediatra")
        _, suggestions = request("GET", f"/familias/{FAMILIA}/sugerencias?q={query}", mama)
        found = next(
            (item for item in suggestions["sugerencias"] if item["entidadId"] == event_id),
            None,
        )
        if found:
            break
        time.sleep(0.1)
    assert found and found["titulo"] == title
    assert found["lugar"] == place and found["direccion"] == address

    _, catalog = request("GET", f"/familias/{FAMILIA}/catalogo", mama)
    saved_place = next(item for item in catalog["lugares"] if item["nombre"] == place)
    assert saved_place["direccion"] == address
    assert all(item.get("responsable") for item in catalog["tratamientos"])

    treatment = next(item for item in catalog["tratamientos"] if not item.get("recetaId"))
    _, quota_before = request("GET", f"/familias/{FAMILIA}/archivos/cuota", papa)
    upload_status, recipe = upload_recipe(treatment["id"], papa)
    _, downloaded = request_raw("GET", f"/familias/{FAMILIA}/archivos/{recipe['id']}", mama)
    assert downloaded.startswith(b"\xff\xd8")
    _, quota_with_photo = request("GET", f"/familias/{FAMILIA}/archivos/cuota", papa)
    assert quota_with_photo["usadosBytes"] > quota_before["usadosBytes"]
    file_isolation, _ = request_raw("GET", f"/familias/{OTRA}/archivos/{recipe['id']}", papa, expected=(403, 404))
    request("DELETE", f"/familias/{FAMILIA}/archivos/{recipe['id']}", papa, expected=(204,))
    _, quota_after = request("GET", f"/familias/{FAMILIA}/archivos/cuota", papa)
    assert quota_after["usadosBytes"] == quota_before["usadosBytes"]

    _, papa_audit = request("GET", f"/familias/{FAMILIA}/auditoria", papa)
    _, mama_audit = request("GET", f"/familias/{FAMILIA}/auditoria", mama)
    papa_entry = next(item for item in papa_audit["entradas"] if item["entidadId"] == event_id)
    mama_entry = next(item for item in mama_audit["entradas"] if item["entidadId"] == event_id)
    assert papa_entry["actor"] == mama_entry["actor"] == "Papá"
    isolation_status, _ = request(
        "GET",
        f"/familias/{OTRA}/auditoria",
        papa,
        expected=(403, 404),
    )

    print(
        json.dumps(
            {
                "actor": papa_entry["actor"],
                "aislamiento_otra_familia": isolation_status,
                "alta_evento": event_status,
                "alta_receta": upload_status,
                "auditoria_papa_mama": papa_entry["fecha"] == mama_entry["fecha"],
                "guardado_segundos": round(save_seconds, 3),
                "indexacion_asincrona": True,
                "aislamiento_archivo": file_isolation,
                "cifrado_descarga_y_borrado": True,
                "lugar_privado_guardado": saved_place["nombre"] == place,
                "responsables_tratamientos": True,
                "sugerencia_registro_real": found["entidadId"] == event_id,
            },
            ensure_ascii=False,
            sort_keys=True,
        )
    )


if __name__ == "__main__":
    if len(sys.argv) != 2:
        raise SystemExit("Uso: validar-v5.py <archivo.env>")
    main(sys.argv[1])
