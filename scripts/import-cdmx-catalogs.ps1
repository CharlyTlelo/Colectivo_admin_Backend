param(
    [string]$SourceUrl = "https://www.correosdemexico.gob.mx/datosabiertos/cp/cpdescarga.txt",
    [string]$MongoUri = "",
    [switch]$DryRun
)

$ErrorActionPreference = "Stop"

function Read-DotEnvValue([string]$path, [string]$key) {
    if (-not (Test-Path -LiteralPath $path)) { return "" }
    foreach ($line in Get-Content -LiteralPath $path -Encoding UTF8) {
        $trimmed = $line.Trim()
        if ($trimmed.Length -eq 0 -or $trimmed.StartsWith("#")) { continue }
        $equals = $trimmed.IndexOf("=")
        if ($equals -lt 1) { continue }
        $name = $trimmed.Substring(0, $equals).Trim()
        if ($name -ne $key) { continue }
        return $trimmed.Substring($equals + 1).Trim().Trim('"').Trim("'")
    }
    return ""
}

function Catalog-Type([string]$sepomexType) {
    switch ($sepomexType.Trim().ToUpperInvariant()) {
        "PUEBLO" { return "PUEBLO" }
        "COLONIA" { return "COLONIA" }
        "BARRIO" { return "BARRIO" }
        default { return "LOCALIDAD" }
    }
}

function To-JsonLiteral($value) {
    return ($value | ConvertTo-Json -Depth 8 -Compress)
}

$repoRoot = Split-Path -Parent $PSScriptRoot
$parentRoot = Split-Path -Parent $repoRoot

if (-not $MongoUri) {
    $MongoUri = $env:MONGODB_URI
}
if (-not $MongoUri) {
    $MongoUri = Read-DotEnvValue (Join-Path $repoRoot ".env") "MONGODB_URI"
}
if (-not $MongoUri) {
    $MongoUri = Read-DotEnvValue (Join-Path $parentRoot ".env") "MONGODB_URI"
}
if (-not $MongoUri) {
    throw "MONGODB_URI not found. Set it in env or .env before running this import."
}

$downloadPath = Join-Path $env:TEMP "colectivo-cpdescarga.txt"
Invoke-WebRequest -Uri $SourceUrl -OutFile $downloadPath -UseBasicParsing

$lines = Get-Content -LiteralPath $downloadPath -Encoding Default
$rows = New-Object System.Collections.Generic.List[object]
$municipalitiesByCode = @{}
$localityKeys = @{}

foreach ($line in ($lines | Select-Object -Skip 2)) {
    if (-not $line.Trim()) { continue }
    $parts = $line -split "\|"
    if ($parts.Count -lt 15) { continue }
    if ($parts[7] -ne "09") { continue }

    $municipalityCode = $parts[11].Trim()
    $municipalityName = $parts[3].Trim()
    $localityName = $parts[1].Trim()
    $localityType = Catalog-Type $parts[2]
    if (-not $municipalityCode -or -not $municipalityName -or -not $localityName) { continue }

    $municipalitiesByCode[$municipalityCode] = $municipalityName
    $key = "$municipalityCode|$localityName"
    if (-not $localityKeys.ContainsKey($key)) {
        $localityKeys[$key] = $true
        $rows.Add([ordered]@{
            municipalityCode = $municipalityCode
            municipalityName = $municipalityName
            name = $localityName
            type = $localityType
        })
    }
}

$municipalities = foreach ($entry in $municipalitiesByCode.GetEnumerator()) {
    [ordered]@{
        code = $entry.Key
        name = $entry.Value
        type = "ALCALDIA"
    }
}

$manualLocalities = @(
    [ordered]@{
        municipalityCode = "009"
        municipalityName = "Milpa Alta"
        name = "San Pablo Oztotepec"
        type = "PUEBLO"
    }
)

foreach ($manual in $manualLocalities) {
    $key = "$($manual.municipalityCode)|$($manual.name)"
    if (-not $localityKeys.ContainsKey($key)) {
        $localityKeys[$key] = $true
        $rows.Add($manual)
    }
}

$summary = [ordered]@{
    source = $SourceUrl
    municipalities = @($municipalities).Count
    localities = $rows.Count
    manualLocalities = $manualLocalities.Count
}

if ($DryRun) {
    $summary | ConvertTo-Json -Depth 4
    exit 0
}

$payload = [ordered]@{
    municipalities = @($municipalities | Sort-Object name)
    localities = @($rows | Sort-Object municipalityName, name)
}

$payloadJson = To-JsonLiteral $payload
$mongoScriptPath = Join-Path $env:TEMP "colectivo-import-cdmx-catalogs.js"

@"
const payload = $payloadJson;
const now = new Date();

db.countries.createIndex({ code: 1 }, { unique: true, name: "countries_code_unique" });
db.states.createIndex({ countryId: 1, name: 1 }, { unique: true, name: "states_country_name_unique" });
db.states.createIndex({ countryId: 1, code: 1 }, { unique: true, name: "states_country_code_unique" });
db.municipalities.createIndex({ stateId: 1, name: 1 }, { unique: true, name: "municipalities_state_name_unique" });
db.localities.createIndex({ municipalityId: 1, name: 1 }, { unique: true, name: "localities_municipality_name_unique" });

db.countries.updateOne(
  { code: "MX" },
  {
    `$setOnInsert: { code: "MX", createdAt: now },
    `$set: { name: "M\u00e9xico", active: true, updatedAt: now }
  },
  { upsert: true }
);
const country = db.countries.findOne({ code: "MX" });

db.states.updateOne(
  { countryId: country._id.toString(), code: "CDMX" },
  {
    `$setOnInsert: { countryId: country._id.toString(), code: "CDMX", createdAt: now },
    `$set: { name: "Ciudad de M\u00e9xico", active: true, updatedAt: now }
  },
  { upsert: true }
);
const state = db.states.findOne({ countryId: country._id.toString(), code: "CDMX" });

const municipalityIds = {};
for (const item of payload.municipalities) {
  db.municipalities.updateOne(
    { stateId: state._id.toString(), name: item.name },
    {
      `$setOnInsert: { stateId: state._id.toString(), name: item.name, createdAt: now },
      `$set: { type: "ALCALDIA", active: true, updatedAt: now }
    },
    { upsert: true }
  );
  municipalityIds[item.code] = db.municipalities.findOne({ stateId: state._id.toString(), name: item.name })._id.toString();
}

let localityCount = 0;
for (const item of payload.localities) {
  const municipalityId = municipalityIds[item.municipalityCode];
  if (!municipalityId) continue;
  db.localities.updateOne(
    { municipalityId, name: item.name },
    {
      `$setOnInsert: { municipalityId, name: item.name, createdAt: now },
      `$set: { type: item.type, active: true, updatedAt: now }
    },
    { upsert: true }
  );
  localityCount++;
}

print(JSON.stringify({
  country: country._id.toString(),
  state: state._id.toString(),
  municipalities: Object.keys(municipalityIds).length,
  localities: localityCount
}));
"@ | Set-Content -LiteralPath $mongoScriptPath -Encoding UTF8

mongosh $MongoUri $mongoScriptPath --quiet
