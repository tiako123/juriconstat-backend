import urllib.request
import json
import sys

api_key = "AIzaSyBI2zoFum1vXae23ngwC6aXlWg3ifzYJe0"
models = [
    "gemini-2.5-flash",
    "gemini-2.5-flash-lite",
    "gemini-1.5-flash",
    "gemini-2.0-flash"
]

print("=== STARTING LIVE GEMINI API TEST ===")
print(f"API Key: {api_key[:10]}...{api_key[-10:]}")

for model in models:
    url = f"https://generativelanguage.googleapis.com/v1beta/models/{model}:generateContent?key={api_key}"
    print(f"\nTesting model: {model}")
    print(f"URL: {url.replace(api_key, 'HIDDEN')}")
    
    payload = {
        "contents": [{
            "parts": [{
                "text": "Bonjour, réponds en un mot pour confirmer ta disponibilité."
            }]
        }],
        "generationConfig": {
            "temperature": 0.3,
            "maxOutputTokens": 50
        }
    }
    
    req = urllib.request.Request(
        url,
        data=json.dumps(payload).encode('utf-8'),
        headers={'Content-Type': 'application/json'},
        method='POST'
    )
    
    try:
        with urllib.request.urlopen(req) as response:
            res_body = response.read().decode('utf-8')
            res_json = json.loads(res_body)
            # Try to extract the text
            text = res_json['candidates'][0]['content']['parts'][0]['text'].strip()
            print(f"[SUCCESS] Response: '{text}'")
    except urllib.error.HTTPError as e:
        print(f"[HTTP ERROR] {e.code}: {e.reason}")
        try:
            err_body = e.read().decode('utf-8')
            print(f"Error details: {err_body}")
        except Exception:
            pass
    except Exception as e:
        print(f"[GENERAL ERROR] {str(e)}")

print("\n=== GEMINI API TEST COMPLETE ===")
