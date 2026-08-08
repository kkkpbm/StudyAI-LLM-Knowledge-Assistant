import uvicorn
import os

os.environ["PYTHONUTF8"] = "1"

if __name__ == "__main__":
    uvicorn.run("app.main:app", host="0.0.0.0", port=8000, reload=True, log_level="info")
