import glob, re, html
for f in glob.glob("app/build/test-results/testDebugUnitTest/TEST-*EnglishDictionary*.xml"):
    txt = open(f, encoding="utf-8").read()
    for m in re.finditer(r'<failure[^>]*message="([^"]{0,500})', txt):
        print(html.unescape(m.group(1)).replace("\n", " | ")[:500])
        print("---")
