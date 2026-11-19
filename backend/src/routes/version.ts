import { Router } from "express";
import { BACKEND_VERSION } from "../version.js";

/** `GET /api/version`: the backend's version, so the app can show which server it talks to. */
export function createVersionRouter(version: string = BACKEND_VERSION): Router {
  const router = Router();
  router.get("/", (_req, res) => {
    res.json({ version });
  });
  return router;
}
