# Pull the Bluetooth HCI snoop log off an Android phone.
#
# Android 9+ keeps the log in /data/misc/bluetooth/logs/, which is not readable over adb.
# A bugreport archive is the supported way out, so grab that and extract the log from it.

param(
    [string]$OutDir = "captures"
)

$ErrorActionPreference = "Stop"

New-Item -ItemType Directory -Path $OutDir -Force | Out-Null

Write-Host "Waiting for device (authorise the USB debugging prompt on the phone)..."
adb wait-for-device
adb devices

$stamp = Get-Date -Format "yyyyMMdd-HHmmss"
$zip = Join-Path $OutDir "bugreport-$stamp.zip"

Write-Host "Taking bugreport, this takes a minute or two..."
adb bugreport $zip

if (-not (Test-Path $zip)) {
    # Some OEM builds append their own name; pick up whatever landed in the folder.
    $zip = Get-ChildItem $OutDir -Filter "*.zip" | Sort-Object LastWriteTime -Descending | Select-Object -First 1 -ExpandProperty FullName
}

Write-Host "Extracting snoop logs from $zip"
Add-Type -AssemblyName System.IO.Compression.FileSystem
$archive = [System.IO.Compression.ZipFile]::OpenRead($zip)
$found = 0
try {
    foreach ($entry in $archive.Entries) {
        if ($entry.FullName -match "btsnoop") {
            $target = Join-Path $OutDir ([System.IO.Path]::GetFileName($entry.FullName))
            [System.IO.Compression.ZipFileExtensions]::ExtractToFile($entry, $target, $true)
            Write-Host "  -> $target"
            $found++
        }
    }
} finally {
    $archive.Dispose()
}

if ($found -eq 0) {
    Write-Warning "No btsnoop file inside the bugreport. The HCI snoop log setting is probably off, or Bluetooth was not restarted after enabling it."
} else {
    Write-Host "Done. Open the .log file in Wireshark and filter: btatt.opcode == 0x52 || btatt.opcode == 0x12"
}
