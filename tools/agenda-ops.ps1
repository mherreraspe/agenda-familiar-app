[CmdletBinding()]
param(
    [Parameter(Position = 0)]
    [ValidateSet('Resumen', 'Estado', 'Entorno', 'VerificarLocal', 'VerificarIntegracion', 'Servidor', 'Desplegar', 'E2EV5')]
    [string]$Accion = 'Resumen',

    [string]$Servidor = '148.116.110.18',
    [string]$UsuarioSsh = 'ubuntu',
    [string]$RaizRemota = '/srv/agenda-familiar',
    [string]$RutaClave,
    [switch]$Detallado
)

Set-StrictMode -Version Latest
$ErrorActionPreference = 'Stop'

$RaizRepositorio = (Resolve-Path (Join-Path $PSScriptRoot '..')).Path
$RaizWorkspace = (Resolve-Path (Join-Path $RaizRepositorio '..')).Path
$Jdk = Join-Path $RaizWorkspace '.local-tools\jdk-25'
$Maven = Join-Path $RaizWorkspace '.local-tools\maven\apache-maven-3.9.11'
$CacheMaven = Join-Path $RaizWorkspace '.m2-cache\repository'
$Temporal = Join-Path $RaizWorkspace '.auth-temp'
$DirectorioLogs = Join-Path $Temporal 'logs'

if ([string]::IsNullOrWhiteSpace($RutaClave)) {
    $RutaClave = Join-Path $RaizWorkspace 'instanciaVM\ssh-key-2026-03-28.key'
}

function Inicializar-Entorno {
    foreach ($ruta in @($Jdk, $Maven)) {
        if (-not (Test-Path -LiteralPath $ruta)) {
            throw "Herramienta local ausente: $ruta"
        }
    }

    $env:JAVA_HOME = $Jdk
    $env:Path = "$(Join-Path $Jdk 'bin');$(Join-Path $Maven 'bin');$env:Path"
    New-Item -ItemType Directory -Force -Path $CacheMaven | Out-Null
}

function Invocar-GitSeguro {
    param([Parameter(ValueFromRemainingArguments = $true)][string[]]$Argumentos)
    & git -c "safe.directory=$($RaizRepositorio.Replace('\', '/'))" -C $RaizRepositorio @Argumentos
    if ($LASTEXITCODE -ne 0) {
        throw "Git terminó con código $LASTEXITCODE"
    }
}

function Invocar-Registrado {
    param(
        [Parameter(Mandatory = $true)][string]$Nombre,
        [Parameter(Mandatory = $true)][scriptblock]$Comando,
        [string[]]$PatronesResumen = @()
    )

    if ($Detallado) {
        $errorAnterior = $ErrorActionPreference
        try {
            $ErrorActionPreference = 'Continue'
            & $Comando
            $codigoDetallado = $LASTEXITCODE
        }
        finally {
            $ErrorActionPreference = $errorAnterior
        }
        if ($codigoDetallado -ne 0) {
            throw "$Nombre terminó con código $codigoDetallado"
        }
        return
    }

    New-Item -ItemType Directory -Force -Path $DirectorioLogs | Out-Null
    $marca = Get-Date -Format 'yyyyMMdd-HHmmss'
    $log = Join-Path $DirectorioLogs "$marca-$Nombre.log"
    $errorAnterior = $ErrorActionPreference
    try {
        $ErrorActionPreference = 'Continue'
        & $Comando 2>&1 | Out-File -FilePath $log -Encoding utf8
        $codigo = $LASTEXITCODE
    }
    finally {
        $ErrorActionPreference = $errorAnterior
    }

    if ($codigo -ne 0) {
        Write-Output "$Nombre=FALLO codigo=$codigo log=$log"
        Get-Content -Encoding UTF8 -Tail 120 $log
        throw "$Nombre falló"
    }

    Write-Output "$Nombre=OK"
    if ($PatronesResumen.Count -gt 0) {
        Select-String -Path $log -Pattern $PatronesResumen -Encoding UTF8 |
            Select-Object -Last 12 |
            ForEach-Object {
                ($_.Line -replace "$([char]27)\[[0-9;]*[A-Za-z]", '').Trim()
            }
    }
}

function Nueva-ClaveTemporal {
    New-Item -ItemType Directory -Force -Path $Temporal | Out-Null
    $destino = Join-Path $Temporal "agenda-familiar-ssh-$PID.key"
    try {
        Copy-Item -LiteralPath $RutaClave -Destination $destino -Force
    }
    catch [System.UnauthorizedAccessException] {
        $identidad = [System.Security.Principal.WindowsIdentity]::GetCurrent().Name
        throw "Sin lectura sobre la clave SSH. Solicitar aprobación para: icacls `"$RutaClave`" /grant `"${identidad}:R`""
    }

    $identidadActual = [System.Security.Principal.WindowsIdentity]::GetCurrent().Name
    & icacls $destino '/inheritance:r' '/grant:r' "${identidadActual}:R" | Out-Null
    if ($LASTEXITCODE -ne 0) {
        throw 'No se pudo restringir la copia temporal de la clave SSH.'
    }
    return $destino
}

function Eliminar-ClaveTemporal {
    param([string]$Clave)
    if (-not (Test-Path -LiteralPath $Clave)) {
        return
    }
    $resuelta = (Resolve-Path -LiteralPath $Clave).Path
    if (-not $resuelta.StartsWith($Temporal, [System.StringComparison]::OrdinalIgnoreCase)) {
        throw "Se rechazó eliminar una clave fuera de $Temporal"
    }
    & icacls $resuelta '/reset' | Out-Null
    Remove-Item -LiteralPath $resuelta -Force
}

function Argumentos-Ssh {
    param([string]$Clave)
    $knownHosts = Join-Path $Temporal 'known_hosts'
    return @(
        '-i', $Clave,
        '-o', 'BatchMode=yes',
        '-o', 'ConnectTimeout=15',
        '-o', 'StrictHostKeyChecking=accept-new',
        '-o', "UserKnownHostsFile=$knownHosts"
    )
}

function Con-ClaveSsh {
    param([scriptblock]$Operacion)
    $clave = Nueva-ClaveTemporal
    try {
        & $Operacion $clave
    }
    finally {
        Eliminar-ClaveTemporal $clave
    }
}

switch ($Accion) {
    'Resumen' {
        Get-Content -Encoding UTF8 (Join-Path $RaizRepositorio 'docs\00-RELEVO_RAPIDO.md')
        Write-Output ''
        Invocar-GitSeguro status --short --branch
    }
    'Estado' {
        Invocar-GitSeguro status --short --branch
        Write-Output "Repositorio=$RaizRepositorio"
    }
    'Entorno' {
        Inicializar-Entorno
        & java -version
        if ($LASTEXITCODE -ne 0) {
            throw 'Java no pudo iniciar. Consultar docs/04-GUIA_OPERATIVA_CODEX.md.'
        }
        & (Join-Path $Maven 'bin\mvn.cmd') -version
        if ($LASTEXITCODE -ne 0) {
            throw 'Maven no pudo iniciar. Consultar docs/04-GUIA_OPERATIVA_CODEX.md.'
        }
    }
    'VerificarLocal' {
        Inicializar-Entorno
        Push-Location (Join-Path $RaizRepositorio 'frontend')
        try {
            Invocar-Registrado 'frontend-test' { & npm.cmd run test } @('Test Files', 'Tests ', 'Duration')
            Invocar-Registrado 'frontend-build' { & npm.cmd run build } @('built in', 'files generated')
        }
        finally {
            Pop-Location
        }

        Push-Location $RaizRepositorio
        try {
            Invocar-Registrado 'maven-verify' {
                & (Join-Path $Maven 'bin\mvn.cmd') "-Dmaven.repo.local=$CacheMaven" verify
            } @('Tests run:', 'BUILD SUCCESS', 'Total time:')
        }
        finally {
            Pop-Location
        }
    }
    'VerificarIntegracion' {
        Inicializar-Entorno
        Push-Location $RaizRepositorio
        try {
            Invocar-Registrado 'maven-integracion' {
                & (Join-Path $Maven 'bin\mvn.cmd') "-Dmaven.repo.local=$CacheMaven" verify -Pintegracion
            } @('Tests run:', 'BUILD SUCCESS', 'Total time:')
        }
        finally {
            Pop-Location
        }
    }
    'Servidor' {
        Con-ClaveSsh {
            param($clave)
            $argumentos = Argumentos-Ssh $clave
            $destino = "$UsuarioSsh@$Servidor"
            $scriptEstado = Join-Path $PSScriptRoot 'servidor\estado.sh'
            Get-Content -Encoding UTF8 $scriptEstado | Select-Object -Skip 1 | & ssh @argumentos $destino "sed '1s/^\xEF\xBB\xBF//; s/`r$//' | bash -s"
            if ($LASTEXITCODE -ne 0) { throw 'Falló la comprobación remota.' }
        }
    }
    'Desplegar' {
        $estado = @(Invocar-GitSeguro status --porcelain)
        if ($estado.Count -gt 0) {
            throw 'El despliegue exige un árbol de trabajo limpio.'
        }
        $commit = (Invocar-GitSeguro rev-parse HEAD).Trim()
        $rama = (Invocar-GitSeguro branch --show-current).Trim()
        if ($rama -ne 'main') {
            throw 'El despliegue solo se permite desde main.'
        }
        New-Item -ItemType Directory -Force -Path $Temporal | Out-Null
        $paquete = Join-Path $Temporal "agenda-familiar-$($commit.Substring(0, 7)).tar.gz"
        try {
            & git -c "safe.directory=$RaizRepositorio" archive --format=tar.gz --output=$paquete HEAD
            if ($LASTEXITCODE -ne 0) { throw 'No se pudo crear el paquete del release.' }
            Con-ClaveSsh {
                param($clave)
                $argumentos = Argumentos-Ssh $clave
                $destino = "$UsuarioSsh@$Servidor"
                $paqueteRemoto = "/tmp/agenda-familiar-$($commit.Substring(0, 7))-$PID.tar.gz"
                $scriptRemoto = "/tmp/agenda-desplegar-$PID.sh"
                $scriptLocal = Join-Path $PSScriptRoot 'servidor\desplegar.sh'
                try {
                    & scp @argumentos $paquete $scriptLocal "${destino}:/tmp/"
                    if ($LASTEXITCODE -ne 0) { throw 'No se pudo copiar el release al servidor.' }
                    & ssh @argumentos $destino "mv /tmp/$([IO.Path]::GetFileName($paquete)) $paqueteRemoto && mv /tmp/desplegar.sh $scriptRemoto && chmod 700 $scriptRemoto && $scriptRemoto $RaizRemota $paqueteRemoto $commit"
                    if ($LASTEXITCODE -ne 0) { throw 'Falló el despliegue remoto.' }
                }
                finally {
                    & ssh @argumentos $destino "rm -f $paqueteRemoto $scriptRemoto" | Out-Null
                }
            }
        }
        finally {
            if (Test-Path -LiteralPath $paquete) { Remove-Item -LiteralPath $paquete -Force }
        }
    }
    'E2EV5' {
        Con-ClaveSsh {
            param($clave)
            $argumentos = Argumentos-Ssh $clave
            $destino = "$UsuarioSsh@$Servidor"
            $local = Join-Path $PSScriptRoot 'e2e\validar-v5.py'
            $remoto = "/tmp/agenda-e2e-v5-$PID.py"
            try {
                & scp @argumentos $local "${destino}:$remoto"
                if ($LASTEXITCODE -ne 0) { throw 'No se pudo copiar el E2E al servidor.' }
                & ssh @argumentos $destino "python3 -m py_compile $remoto && python3 $remoto $RaizRemota/current/infraestructura/.env"
                if ($LASTEXITCODE -ne 0) { throw 'Falló el E2E V5.' }
            }
            finally {
                & ssh @argumentos $destino "rm -f $remoto" | Out-Null
            }
        }
    }
}
