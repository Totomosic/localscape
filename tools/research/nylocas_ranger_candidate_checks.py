#!/usr/bin/env python3
"""Compile isolated Java policy candidates and selected ranger JUnit checks, without Gradle."""
import argparse
from pathlib import Path
import subprocess


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--source', required=True, type=Path)
    parser.add_argument('--output', required=True, type=Path)
    parser.add_argument('tests', nargs='+')
    args = parser.parse_args()
    root = Path(__file__).resolve().parents[2]
    dependencies = [root/'.state/m2/junit/junit/4.12/junit-4.12.jar',
        root/'.state/m2/org/hamcrest/hamcrest-core/1.3/hamcrest-core-1.3.jar',
        root/'.state/m2/com/google/code/gson/gson/2.10.1/gson-2.10.1.jar']
    classpath = ':'.join(map(str, dependencies))
    args.output.mkdir(parents=True, exist_ok=True)
    sources = sorted(args.source.rglob('*.java'))
    sources += [root/'client/src/test/java/org/localscape/nylocas/ranger'/(name+'.java') for name in args.tests]
    subprocess.run(['javac', '-cp', classpath, '-d', str(args.output), *map(str, sources)], check=True)
    result = subprocess.run(['java', '-cp', str(args.output)+':'+classpath,
        'org.junit.runner.JUnitCore', *['org.localscape.nylocas.ranger.'+name for name in args.tests]])
    raise SystemExit(result.returncode)


if __name__ == '__main__':
    main()
