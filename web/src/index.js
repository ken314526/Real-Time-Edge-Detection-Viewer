const frameImage = document.getElementById("frame");
const fpsLabel = document.getElementById("fps");
const resolutionLabel = document.getElementById("resolution");
const timestampLabel = document.getElementById("timestamp");

const SAMPLE_BASE64 =
  "data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAAGQAAABkCAYAAABw4pVUAAABb0lEQVR4nO3QsQ3CMAxF0dnhEzgKwC+zA0RduCMsZIXTWdKtx55BHmTyP89Pn8/Hk+1n0nIszQAAAAAAAAAAAOx5MzsMxmk1z7NmBGG99nmCaTKfsykCQ6ns6XBZlhGVo1nZuNHPs+bI13bcy4RFki6xHKMcPn8fgqS5P0PXR0A8klSeQzj1kdr1xH0DglnWeEca8z3sI3Al6zxjFFW8zx7CdwJ+s8YxRVvM8ewncCfrPGMUVbzPHsJ3An6zxjFFW8zx7CdwJ+s8YxRVvM8ewncCfrPGMUVbzPHsJ3An6zxjFFW8zx7CdwJ+s8YxRVvM8ezl8vl8vk+L1X3uF7S57k3SXqPv8XbTnsm6S9T7/G203bJukvU+/xttN2ybpL1Pv8bbTdsu6X9z4ZoAAAAAAAAAAABAPXUTTCgCXpV3ygAAAABJRU5ErkJggg==";

function updateFrame() {
  frameImage.src = SAMPLE_BASE64;
  const now = new Date();
  fpsLabel.textContent = "12.5";
  resolutionLabel.textContent = "1280×720";
  timestampLabel.textContent = now.toLocaleTimeString();
}

document.addEventListener("DOMContentLoaded", () => {
  updateFrame();
  const refreshButton = document.getElementById("refresh");
  refreshButton.addEventListener("click", () => {
    updateFrame();
  });
});
