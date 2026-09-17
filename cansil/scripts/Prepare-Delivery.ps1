param([string]$JarPath)
$ErrorActionPreference = 'Stop'
$projectRoot = [IO.Path]::GetFullPath((Join-Path $PSScriptRoot '..'))
$deliveryDir = Join-Path $projectRoot 'dist'
$null = New-Item -ItemType Directory -Path $deliveryDir -Force
$stamp = Get-Date -Format 'yyyyMMdd-HHmmss'
$archivePath = Join-Path $deliveryDir "fundo-cansil-backend-candidato-$stamp.zip"
if (Test-Path -LiteralPath $archivePath) { throw 'Já existe um pacote com esse nome.' }

# Lista positiva: nunca empacotar .env real, Git, banco, logs ou arquivos da IDE.
$rootFiles = @('pom.xml','mvnw','mvnw.cmd','Dockerfile','compose.yaml','.gitignore','.dockerignore','.env.example','INVESTIMENTOS.md','ENTREGA.md','FRONTEND.md','DOCKER.md')
$items = [Collections.Generic.List[IO.FileInfo]]::new()
foreach ($name in $rootFiles) {
    $path = Join-Path $projectRoot $name
    if (Test-Path -LiteralPath $path -PathType Leaf) { $items.Add((Get-Item -LiteralPath $path -Force)) }
}
foreach ($folder in @('src','openspec','.mvn','scripts','docs','docker')) {
    $path = Join-Path $projectRoot $folder
    if (Test-Path -LiteralPath $path) {
        Get-ChildItem -LiteralPath $path -Recurse -File -Force | Where-Object {
            $_.Name -notlike '.env*' -and $_.Extension -notin @('.log','.dump','.zip','.jar') -and
            $_.LinkType -notin @('SymbolicLink','Junction')
        } | ForEach-Object { $items.Add($_) }
    }
}

Add-Type -AssemblyName System.IO.Compression.FileSystem
$archive = [IO.Compression.ZipFile]::Open($archivePath,[IO.Compression.ZipArchiveMode]::Create)
try {
    foreach ($item in $items) {
        $entryName = $item.FullName.Substring($projectRoot.Length + 1).Replace('\','/')
        $null = [IO.Compression.ZipFileExtensions]::CreateEntryFromFile($archive,$item.FullName,$entryName)
    }
    if ($JarPath) {
        $jar = Get-Item -LiteralPath $JarPath
        if ($jar.Extension -ne '.jar') { throw 'Informe um JAR produzido pelo build verificado.' }
        $null = [IO.Compression.ZipFileExtensions]::CreateEntryFromFile($archive,$jar.FullName,'bin/fundo-cansil.jar')
    }
} finally { $archive.Dispose() }

$check = [IO.Compression.ZipFile]::OpenRead($archivePath)
try {
    $invalid = @($check.Entries | Where-Object {
        $_.FullName -match '(^|/)(\.git|\.idea|\.env)(/|$)' -or $_.FullName -match '\.(log|dump)$'
    })
    if ($invalid.Count -gt 0) { throw 'Pacote reprovado: contém caminho não permitido.' }
    if (-not ($check.Entries.FullName -contains 'src/main/java/com/curso/services/AuthService.java')) { throw 'Código-fonte ausente do pacote.' }
    if (-not ($check.Entries.FullName -contains '.env.example')) { throw 'Configuração de exemplo ausente do pacote.' }
    Write-Output ('ARQUIVOS: ' + $check.Entries.Count)
} finally { $check.Dispose() }
Write-Output ('PACOTE: ' + $archivePath)
Write-Output ('SHA256: ' + (Get-FileHash -LiteralPath $archivePath -Algorithm SHA256).Hash)
Write-Output 'Pacote somente do backend. Para subir o Compose completo, mantenha também a pasta irmã frontend com Dockerfile e fontes; veja DOCKER.md.'
