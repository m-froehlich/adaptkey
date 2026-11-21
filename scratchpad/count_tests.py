import glob, re
files = glob.glob("app/build/test-results/testDebugUnitTest/*.xml")
n = sum(int(re.search(r'tests="(\d+)"', open(f, encoding="utf-8").read()).group(1)) for f in files)
fails = sum(int(re.search(r'failures="(\d+)"', open(f, encoding="utf-8").read()).group(1)) for f in files)
print(f"{n} tests, {fails} failures")
