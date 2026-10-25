"""Summarize JDK 21 JFR samples without adding instrumentation to the shipped mod.

Inclusive sample shares are NOT wall-clock timings, GPU timings or FPS gains.
Run after a timed recording completes. Requires only Python's standard library and jfr.
"""
import argparse
from collections import Counter, defaultdict
import json
from pathlib import Path
import subprocess
import tempfile

PREFIX = 'net.chriskatze.katzencraftmetals.'


def method_name(frame):
    method = frame.get('method') or {}
    owner = (method.get('type') or {}).get('name', '').replace('/', '.')
    return owner + '.' + method.get('name', '?')


def summarize(events):
    totals = Counter()
    inclusive = defaultdict(Counter)
    leaves = defaultdict(Counter)
    allocated = Counter()
    allocation_samples = 0
    for event in events:
        values = event.get('values') or {}
        frames = ((values.get('stackTrace') or {}).get('frames') or [])
        methods = [method_name(frame) for frame in frames]
        if event.get('type') in ('jdk.ExecutionSample', 'jdk.NativeMethodSample'):
            thread = values.get('sampledThread') or {}
            key = thread.get('javaName') or thread.get('osName') or 'unknown'
            totals[key] += 1
            inclusive[key].update(set(m for m in methods if m.startswith(PREFIX)))
            if methods and methods[0].startswith(PREFIX):
                leaves[key][methods[0]] += 1
        elif event.get('type') == 'jdk.ObjectAllocationSample':
            allocation_samples += 1
            # JFR weight is an estimated byte count, not the allocated object's exact size.
            weight = values.get('weight', 0)
            if not isinstance(weight, (int, float)):
                continue
            nearest = next((m for m in methods if m.startswith(PREFIX)), None)
            if nearest:
                allocated[nearest] += weight
    lines = ['JFR CPU/allocation sample summary (not a wall-clock/GPU/FPS benchmark)',
             'Inclusive methods overlap; do not sum percentages. Truncated stacks and unsampled work may be absent.',
             'Execution/native samples include on-CPU activity and possible native driver waits; inspect stacks before attribution.']
    for thread, count in sorted(totals.items()):
        lines.append(f'\nThread {thread}: {count} execution/native samples')
        for method, hits in inclusive[thread].most_common(20):
            lines.append(f'  inclusive {hits:6} / {count} ({hits/count:.2%}); leaf {leaves[thread][method]:6}: {method}')
        if not inclusive[thread]:
            lines.append('  No Katzencraft frames sampled (not proof of zero overhead).')
    lines.append(f'\nAllocation sample events: {allocation_samples}; estimated weighted bytes by nearest Katzencraft caller:')
    for method, size in allocated.most_common(20):
        lines.append(f'  {size:,.0f} estimated bytes: {method}')
    if not allocated:
        lines.append('  None attributed; check event configuration, sample count and stack depth.')
    if not totals:
        lines.append('\nNo execution/native samples found. Do not interpret this recording as a performance baseline.')
    return '\n'.join(lines) + '\n'


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('recording', type=Path)
    parser.add_argument('--jfr', default='jfr', help='Path to JDK 21 jfr executable')
    args = parser.parse_args()
    recording = args.recording.resolve(strict=True)
    output = recording.with_suffix('.summary.txt')
    if output.exists():
        parser.error(f'Refusing to overwrite {output}; move it aside before regenerating.')
    # JSON export can be large: write subprocess output directly to a temporary disk file.
    with tempfile.TemporaryFile() as exported:
        subprocess.run([args.jfr, 'print', '--json', '--stack-depth', '128', '--events',
                        'jdk.ExecutionSample,jdk.NativeMethodSample,jdk.ObjectAllocationSample',
                        str(recording)], stdout=exported, check=True)
        exported.seek(0)
        data = json.load(exported)
    output.write_text(summarize(data.get('recording', {}).get('events', [])), encoding='utf-8')
    print(output)


if __name__ == '__main__':
    main()
