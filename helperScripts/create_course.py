from urllib import request, parse
import json

course = {
    "id": 1, "shortName": "prop_french_1", "title": "Propädeutikum Französisch 1"
}

url = 'http://localhost:9090/api/courses'

req = request.Request(url, data=json.dumps(course).encode("utf-8"))

resp = request.urlopen(req)

print(resp.read())
