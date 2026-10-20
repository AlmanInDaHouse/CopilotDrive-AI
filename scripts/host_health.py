"""Bounded Windows host observations; conservative measurement guard, not a CI budget."""
import json
import subprocess
from qualify_android import QualificationError


def assess(samples):
    if len(samples) < 3 or any(any(type(s.get(k)) not in (int, float) or s[k] < 0
        for k in ('availableMiB', 'commitHeadroomMiB', 'pagesInputPerSec', 'cpuPercent')) for s in samples):
        return 'HOST_SAMPLES_UNAVAILABLE'
    if min(s['availableMiB'] for s in samples) < 512:
        return 'HOST_PHYSICAL_MEMORY_PRESSURE'
    if min(s['commitHeadroomMiB'] for s in samples) < 1024:
        return 'HOST_COMMIT_EXHAUSTION_RISK'
    if all(s['pagesInputPerSec'] > 1000 for s in samples):
        return 'HOST_SUSTAINED_PAGING'
    return None


def observe():
    script = """$samples = @(1..3 | ForEach-Object {
      $m = Get-CimInstance Win32_PerfFormattedData_PerfOS_Memory
      $c = Get-CimInstance Win32_PerfFormattedData_PerfOS_Processor -Filter \"Name='_Total'\"
      [pscustomobject]@{availableMiB=[double]$m.AvailableMBytes;
        commitHeadroomMiB=([double]$m.CommitLimit-[double]$m.CommittedBytes)/1MB;
        pagesInputPerSec=[double]$m.PagesInputPersec; cpuPercent=[double]$c.PercentProcessorTime}
      Start-Sleep -Seconds 1
    }); ConvertTo-Json -InputObject $samples -Compress"""
    try:
        run = subprocess.run(['powershell', '-NoProfile', '-Command', script], capture_output=True, timeout=30)
        if run.returncode:
            raise ValueError()
        samples = json.loads(run.stdout)
        return dict(samples=samples, invalidReason=assess(samples),
            method='3 Windows CIM observations at 1s intervals; conservative local guard')
    except (ValueError, OSError, subprocess.TimeoutExpired):
        raise QualificationError('HOST_OBSERVATION_UNAVAILABLE') from None


if __name__ == '__main__':
    print(json.dumps(observe(), indent=2))
