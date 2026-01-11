from fastapi import FastAPI, Request
from fastapi.responses import HTMLResponse, FileResponse
import uvicorn
from datetime import datetime

app = FastAPI()

records = []


@app.post("/upload")
async def upload(request: Request):
    data = await request.json()

    data["formatted_time"] = datetime.now().strftime("%H:%M:%S")
    data["free_memory_mb"] = data.get("free_memory", data.get("free_memory_mb", 0))

    records.append(data)
    if len(records) > 200:
        records.pop(0)

    return {"status": "ok"}


@app.get("/", response_class=HTMLResponse)
def index():
    return FileResponse("index.html")


@app.get("/data")
def get_data():
    return list(reversed(records))


if __name__ == "__main__":
    uvicorn.run(app, host="0.0.0.0", port=8080)
