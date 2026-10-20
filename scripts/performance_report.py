"""Compare external debug measurements only when their recorded conditions match."""
import argparse
import json
from pathlib import Path
from qualify_android import summary, QualificationError

METRICS = ('activityStartMs', 'dashboardReadyUpperBoundMs', 'memoryPssKb')


def compare(before, after):
    for report in (before, after):
        if report.get('status') != 'EMULATOR_TESTED' or report.get('environment', {}).get('kind') != 'EMULATOR':
            raise QualificationError('SUCCESSFUL_EMULATOR_REPORT_REQUIRED')
    if before['environment'] != after['environment']:
        raise QualificationError('ENVIRONMENT_NOT_COMPARABLE')
    for key in ('method', 'conditions'):
        if before['performance'][key] != after['performance'][key]:
            raise QualificationError('METHOD_NOT_COMPARABLE')
    result = dict(physicalQualification='PENDING', advisoryOnly=True,
        metricKind='EXTERNAL_DEBUG_ACTIVITY_UI_BOUND_AND_PSS', metrics={})
    for metric in METRICS:
        left = [item[metric] for item in before['performance']['samples']]
        right = [item[metric] for item in after['performance']['samples']]
        if any(type(value) is not int or value < 0 for value in left + right):
            raise QualificationError('INVALID_SAMPLE')
        a, b = summary(left), summary(right)
        entry = dict(before=a, after=b)
        if 'p50' in a and 'p50' in b and a['p50']:
            entry['p50ChangePercent'] = round((b['p50'] / a['p50'] - 1) * 100, 2)
        result['metrics'][metric] = entry
    return result


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--before', type=Path, required=True)
    parser.add_argument('--after', type=Path, required=True)
    parser.add_argument('--out', type=Path, required=True)
    args = parser.parse_args()
    result = compare(json.loads(args.before.read_text()), json.loads(args.after.read_text()))
    args.out.parent.mkdir(parents=True, exist_ok=True)
    args.out.write_text(json.dumps(result, indent=2) + '\n', encoding='utf-8')
    print(json.dumps(result))


if __name__ == '__main__':
    main()
