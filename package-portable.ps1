param(
    [string]$OutputPath = (Join-Path (Split-Path -Parent $PSScriptRoot) "Parking Payment System-portable.zip")
)

$ErrorActionPreference = "Stop"
$projectRoot = [System.IO.Path]::GetFullPath($PSScriptRoot)
$output = [System.IO.Path]::GetFullPath($OutputPath)
$stageRoot = Join-Path ([System.IO.Path]::GetTempPath()) ("parking-payment-package-" + [Guid]::NewGuid())
$stageProject = Join-Path $stageRoot "Parking Payment System"

try {
    New-Item -ItemType Directory -Path $stageProject | Out-Null

    foreach ($item in Get-ChildItem -LiteralPath $projectRoot -Force) {
        if ($item.Name -eq "target") {
            continue
        }

        if ($item.Name -eq "database") {
            $stageDatabase = Join-Path $stageProject "database"
            New-Item -ItemType Directory -Path $stageDatabase | Out-Null
            Get-ChildItem -LiteralPath $item.FullName -Force |
                Where-Object { $_.Name -notin @("SmartParking.mdf", "SmartParking_log.ldf") } |
                ForEach-Object {
                    Copy-Item -LiteralPath $_.FullName -Destination $stageDatabase -Recurse -Force
                }
            continue
        }

        Copy-Item -LiteralPath $item.FullName -Destination $stageProject -Recurse -Force
    }

    $outputDirectory = Split-Path -Parent $output
    if (-not (Test-Path -LiteralPath $outputDirectory)) {
        New-Item -ItemType Directory -Path $outputDirectory | Out-Null
    }

    Add-Type -AssemblyName System.IO.Compression.FileSystem
    if (Test-Path -LiteralPath $output) {
        Remove-Item -LiteralPath $output -Force
    }
    [System.IO.Compression.ZipFile]::CreateFromDirectory(
        $stageRoot,
        $output,
        [System.IO.Compression.CompressionLevel]::Optimal,
        $false
    )

    Write-Output "Portable package created: $output"
} finally {
    if (Test-Path -LiteralPath $stageRoot) {
        $resolvedStage = [System.IO.Path]::GetFullPath($stageRoot)
        $tempRoot = [System.IO.Path]::GetFullPath([System.IO.Path]::GetTempPath())
        if ($resolvedStage.StartsWith($tempRoot, [System.StringComparison]::OrdinalIgnoreCase)) {
            Remove-Item -LiteralPath $resolvedStage -Recurse -Force
        }
    }
}
