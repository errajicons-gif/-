const { onRequest } = require("firebase-functions/v2/https");
const { createApp } = require("./server");

const app = createApp();

// Deploy as HTTPS Cloud Function:
//   firebase deploy --only functions
exports.api = onRequest(
  {
    region: "europe-west1",
    memory: "512MiB",
    timeoutSeconds: 60,
    maxInstances: 10
  },
  app
);
