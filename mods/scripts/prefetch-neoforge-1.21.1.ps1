param(
    [string] $NeoForgeVersion = '21.1.235',
    [string] $NeoFormRuntimeVersion = '2.0.18'
)

$ErrorActionPreference = 'Stop'

$NeoForgeMaven = $env:NEOFORGE_MAVEN_URL
if ([string]::IsNullOrWhiteSpace($NeoForgeMaven)) {
    $NeoForgeMaven = 'https://maven.neoforged.net/releases'
}

$NeoForgeMojangMeta = $env:NEOFORGE_MOJANG_META_URL
if ([string]::IsNullOrWhiteSpace($NeoForgeMojangMeta)) {
    $NeoForgeMojangMeta = 'https://maven.neoforged.net/mojang-meta'
}

$ForgeMaven = $env:FORGE_MAVEN_URL
if ([string]::IsNullOrWhiteSpace($ForgeMaven)) {
    $ForgeMaven = 'https://maven.minecraftforge.net'
}

$script:Seen = @{}

function Get-VersionString($versionObject) {
    if ($null -eq $versionObject) {
        return $null
    }
    if ($versionObject -is [string]) {
        return $versionObject
    }
    if ($versionObject.requires) {
        return [string] $versionObject.requires
    }
    if ($versionObject.strictly) {
        return [string] $versionObject.strictly
    }
    if ($versionObject.prefers) {
        return [string] $versionObject.prefers
    }
    return $null
}

function Get-RepositoryBase($group, $artifact) {
    if ($group -eq 'net.neoforged' -and $artifact -eq 'minecraft-dependencies') {
        return $NeoForgeMojangMeta.TrimEnd('/')
    }
    if ($group -like 'net.neoforged*' -or $group -eq 'cpw.mods' -or $group -eq 'io.github.llamalad7') {
        return $NeoForgeMaven.TrimEnd('/')
    }
    if ($group -like 'net.minecraftforge*' -or $group -eq 'io.codechicken') {
        return $ForgeMaven.TrimEnd('/')
    }
    return $null
}

function Save-Artifact($baseUrl, $group, $artifact, $version, $classifier, $extension) {
    if ([string]::IsNullOrWhiteSpace($baseUrl) -or [string]::IsNullOrWhiteSpace($version)) {
        return $false
    }

    $groupPath = $group.Replace('.', '/')
    $fileName = if ([string]::IsNullOrWhiteSpace($classifier)) {
        "$artifact-$version.$extension"
    } else {
        "$artifact-$version-$classifier.$extension"
    }
    $url = "$baseUrl/$groupPath/$artifact/$version/$fileName"
    $destDir = Join-Path $env:USERPROFILE ".m2\repository\$($group.Replace('.', '\'))\$artifact\$version"
    $dest = Join-Path $destDir $fileName

    if (Test-Path $dest) {
        return $true
    }

    New-Item -ItemType Directory -Force -Path $destDir | Out-Null
    Write-Host "Downloading $url"
    try {
        Invoke-WebRequest -UseBasicParsing $url -OutFile $dest
        return $true
    } catch {
        if (Test-Path $dest) {
            Remove-Item -LiteralPath $dest -Force
        }
        Write-Host "Skipped $url"
        return $false
    }
}

function Save-ModuleFiles($baseUrl, $group, $artifact, $version) {
    [void] (Save-Artifact $baseUrl $group $artifact $version '' 'pom')
    $hasModule = Save-Artifact $baseUrl $group $artifact $version '' 'module'
    $modulePath = Join-Path $env:USERPROFILE ".m2\repository\$($group.Replace('.', '\'))\$artifact\$version\$artifact-$version.module"

    if ($hasModule -and (Test-Path $modulePath)) {
        $metadata = Get-Content -Raw $modulePath | ConvertFrom-Json
        $destDir = Split-Path $modulePath
        foreach ($variant in @($metadata.variants)) {
            foreach ($file in @($variant.files)) {
                if ($null -eq $file) {
                    continue
                }
                $dest = Join-Path $destDir $file.name
                if (Test-Path $dest) {
                    continue
                }
                $url = "$baseUrl/$($group.Replace('.', '/'))/$artifact/$version/$($file.url)"
                Write-Host "Downloading $url"
                try {
                    Invoke-WebRequest -UseBasicParsing $url -OutFile $dest
                } catch {
                    if (Test-Path $dest) {
                        Remove-Item -LiteralPath $dest -Force
                    }
                    Write-Host "Skipped $url"
                }
            }
        }
    } else {
        [void] (Save-Artifact $baseUrl $group $artifact $version '' 'jar')
    }
}

function Ensure-Coordinate($group, $artifact, $version) {
    $baseUrl = Get-RepositoryBase $group $artifact
    if ([string]::IsNullOrWhiteSpace($baseUrl) -or [string]::IsNullOrWhiteSpace($version)) {
        return
    }

    $key = "$group`:$artifact`:$version"
    if ($script:Seen.ContainsKey($key)) {
        return
    }
    $script:Seen[$key] = $true

    Save-ModuleFiles $baseUrl $group $artifact $version

    $modulePath = Join-Path $env:USERPROFILE ".m2\repository\$($group.Replace('.', '\'))\$artifact\$version\$artifact-$version.module"
    if (-not (Test-Path $modulePath)) {
        return
    }

    $metadata = Get-Content -Raw $modulePath | ConvertFrom-Json
    foreach ($variant in @($metadata.variants)) {
        foreach ($dependency in @($variant.dependencies)) {
            if ($null -eq $dependency) {
                continue
            }

            $dependencyGroup = [string] $dependency.group
            $dependencyArtifact = [string] $dependency.module
            $dependencyVersion = Get-VersionString $dependency.version
            $dependencyBaseUrl = Get-RepositoryBase $dependencyGroup $dependencyArtifact

            if ($dependency.thirdPartyCompatibility -and $dependency.thirdPartyCompatibility.artifactSelector -and $dependencyBaseUrl) {
                $selector = $dependency.thirdPartyCompatibility.artifactSelector
                $classifier = [string] $selector.classifier
                $extension = if ($selector.extension) { [string] $selector.extension } else { 'jar' }
                [void] (Save-Artifact $dependencyBaseUrl $dependencyGroup $dependencyArtifact $dependencyVersion $classifier $extension)
            }

            Ensure-Coordinate $dependencyGroup $dependencyArtifact $dependencyVersion
        }
    }
}

Ensure-Coordinate 'net.neoforged' 'neoforge' $NeoForgeVersion
Ensure-Coordinate 'net.neoforged' 'neoform-runtime' $NeoFormRuntimeVersion
[void] (Save-Artifact $ForgeMaven.TrimEnd('/') 'net.minecraftforge' 'mergetool' '1.1.7' 'api' 'jar')

Write-Host "Prefetched $($script:Seen.Count) hosted coordinates."
