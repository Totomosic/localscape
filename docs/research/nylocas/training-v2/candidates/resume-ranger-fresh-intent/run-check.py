#!/usr/bin/env python3
"""Run isolated newer-work intent checks. --baseline intentionally fails the new behavior tests."""
from pathlib import Path
import argparse
import subprocess

parser = argparse.ArgumentParser(description=__doc__)
parser.add_argument("--baseline", action="store_true")
args = parser.parse_args()
here = Path(__file__).resolve().parent
repo = here.parents[2]
profile = "baseline" if args.baseline else "candidate"
jdk = repo / ".state/tools/jdk-21/bin"
jars = [repo / ".state/m2" / path for path in (
    "junit/junit/4.12/junit-4.12.jar",
    "org/hamcrest/hamcrest-core/1.3/hamcrest-core-1.3.jar",
    "com/google/code/gson/gson/2.10.1/gson-2.10.1.jar",
)]
classes = here / (profile + "-classes")
classes.mkdir(exist_ok=True)
source = here / profile
java = source / "policy-source/org/localscape/nylocas"
test_source = here / "candidate/test-source/org/localscape/nylocas"
subprocess.run([str(jdk / "javac"), "-cp", ":".join(map(str, jars)), "-d", str(classes),
    *map(str, java.glob("*.java")), *map(str, (java / "ranger").glob("*.java")),
    *map(str, (java / "mage").glob("*.java")),
    *map(str, (test_source / "ranger").glob("*.java")), *map(str, (test_source / "mage").glob("*.java")), str(here / "RangerFreshIntentTest.java")],
    cwd=repo, check=True)
result = subprocess.run([str(jdk / "java"), "-cp", ":".join(map(str, [classes, *jars])),
    "org.junit.runner.JUnitCore", "org.localscape.nylocas.ranger.RangerStrategyTest",
    "org.localscape.nylocas.ranger.RangerReplayTest", "org.localscape.nylocas.mage.MageStrategyTest",
    "org.localscape.nylocas.mage.MageReplayTest", "org.localscape.nylocas.ranger.RangerFreshIntentTest"],
    cwd=repo, text=True, stdout=subprocess.PIPE, stderr=subprocess.STDOUT)
(here / ("canonical-red.log" if args.baseline else "candidate-check.log")).write_text(result.stdout)
print(result.stdout)
raise SystemExit(result.returncode)
