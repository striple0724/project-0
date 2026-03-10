# BULK INSERT API 대량데이터(10만~20만건) 스트리밍 테스트 가이드

작성일: 2026-03-10  
대상 API: `POST /api/v1/work-items:bulk-file`

## 1. 목적
- 10만~20만건 CSV를 업로드해 BULK INSERT 안정성을 검증합니다.
- 업로드/처리 상태/실패 리포트 다운로드까지 엔드투엔드로 확인합니다.

## 2. 전제 조건
- 백엔드 실행: `http://localhost:19100`
- 로그인 계정: `admin / admin1234`
- 활성 고객사(`ACTIVE`) 데이터가 존재해야 성공률이 높습니다.

## 3. CSV 포맷 (필수)
헤더는 아래 순서와 이름을 사용해야 합니다.

```csv
requestId,clientId,type,assignee,dueDate,tags,memo
```

주의사항:
- `requestId`: 파일 전체에서 동일 값 사용
- `clientId`: 존재하는 고객사 ID
- `type`: `FILING|BOOKKEEPING|REVIEW|ETC`
- `dueDate`: `yyyy-MM-dd`
- `tags`: `|` 구분(예: `VAT|Q1`)
- `memo`에 쉼표(,)는 피하는 것을 권장 (파서가 단순 split 사용)

## 4. PowerShell 테스트 절차 (권장)

### 4.1 로그인 + ACTIVE client 조회 + 10만건 CSV 생성
```powershell
$base = "http://localhost:19100"
$session = New-Object Microsoft.PowerShell.Commands.WebRequestSession

# 로그인
$loginBody = @{ userId = "admin"; password = "admin1234" } | ConvertTo-Json
Invoke-RestMethod -Uri "$base/api/v1/auth/login" -Method Post -ContentType "application/json" -Body $loginBody -WebSession $session | Out-Null

# ACTIVE client 조회
$clientsResp = Invoke-RestMethod -Uri "$base/api/v1/clients?status=ACTIVE&page=0&size=2000" -Method Get -WebSession $session
$clientIds = @($clientsResp.data | ForEach-Object { $_.id })
if ($clientIds.Count -eq 0) { throw "ACTIVE client가 없습니다." }

# CSV 생성 (rows=100000 또는 200000)
$rows = 100000
$requestId = "bulk-test-" + (Get-Date -Format "yyyyMMddHHmmss")
$outFile = "$PWD\bulk-$rows.csv"
$writer = New-Object System.IO.StreamWriter($outFile, $false, [System.Text.UTF8Encoding]::new($true))
try {
  $writer.WriteLine("requestId,clientId,type,assignee,dueDate,tags,memo")
  for ($i=1; $i -le $rows; $i++) {
    $cid = $clientIds[$i % $clientIds.Count]
    $due = (Get-Date).AddDays(($i % 10) + 1).ToString("yyyy-MM-dd")
    $tags = "VAT|Q" + (($i % 4) + 1)
    $memo = "bulk_row_$i"
    $writer.WriteLine("$requestId,$cid,FILING,load-test-user,$due,$tags,$memo")
  }
}
finally { $writer.Close() }
```

### 4.2 BULK 업로드 호출
```powershell
$resp = Invoke-RestMethod -Uri "$base/api/v1/work-items:bulk-file" `
  -Method Post `
  -Form @{ file = Get-Item $outFile; requestId = $requestId } `
  -WebSession $session

$jobId = $resp.data.jobId
"jobId=$jobId"
```

### 4.3 진행상태 확인
```powershell
# 관리자 Job 목록
Invoke-RestMethod -Uri "$base/api/v1/admin/jobs?requestId=$requestId&page=0&size=20" -Method Get -WebSession $session

# 워크벤치 집계 상태
Invoke-RestMethod -Uri "$base/api/v1/workbench/tracking" -Method Get -WebSession $session
```

### 4.4 실패 행 리포트 다운로드 (실패건이 있을 때)
```powershell
Invoke-WebRequest -Uri "$base/api/v1/work-items/bulk-jobs/$jobId/failures" `
  -Method Get -WebSession $session `
  -OutFile "$PWD\bulk-failures-$jobId.csv"
```

## 5. PowerShell에서 스트리밍 테스트가 어려운가?
결론: **불가능하지 않으며, 충분히 가능합니다.**

1. `Invoke-RestMethod -Form` 방식
- 구현이 가장 쉽고, 10~20만건 CSV(수십 MB) 테스트에 실무적으로 충분합니다.
- 다만 내부 구현 세부(메모리 사용 제어, chunked 전송 제어)는 제한적입니다.

2. 진짜 스트림 업로드 제어가 필요한 경우
- .NET `HttpClient` + `StreamContent`로 파일 스트림을 직접 전송하면 더 정밀한 제어가 가능합니다.

예시:
```powershell
$handler = New-Object System.Net.Http.HttpClientHandler
$handler.CookieContainer = New-Object System.Net.CookieContainer
$client = New-Object System.Net.Http.HttpClient($handler)

# (필요 시 로그인 쿠키 선행 세팅)
# 로그인 요청 후 handler.CookieContainer에 세션 쿠키 저장

$fs = [System.IO.File]::OpenRead("$PWD\bulk-100000.csv")
try {
  $content = New-Object System.Net.Http.MultipartFormDataContent
  $fileContent = New-Object System.Net.Http.StreamContent($fs)
  $fileContent.Headers.ContentType = [System.Net.Http.Headers.MediaTypeHeaderValue]::Parse("text/csv")
  $content.Add($fileContent, "file", "bulk-100000.csv")
  $content.Add((New-Object System.Net.Http.StringContent("bulk-test-20260310-001")), "requestId")

  $resp = $client.PostAsync("http://localhost:19100/api/v1/work-items:bulk-file", $content).Result
  $resp.Content.ReadAsStringAsync().Result
}
finally {
  $fs.Dispose()
  $client.Dispose()
}
```

## 6. 권장 검증 쿼리 (H2)
```sql
SELECT COUNT(*) FROM work_items WHERE assignee = 'load-test-user';
SELECT COUNT(*) FROM work_item_audits;
```

## 7. 운영 팁
- 처음은 10만건으로 시작 후 20만건으로 확장
- `requestId`를 실행마다 고유하게 생성
- 실패 리포트 CSV를 반드시 보관해 데이터 품질 이슈를 역추적
