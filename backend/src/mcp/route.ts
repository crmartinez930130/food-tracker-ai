import { Router, type Request, type Response } from "express";
import { NodeStreamableHTTPServerTransport } from "@modelcontextprotocol/node";
import type { AppDatabase } from "../db/client.js";
import { logger } from "../logger.js";
import { createMcpServer } from "./server.js";

function jsonRpcError(res: Response, status: number, code: number, message: string): void {
  res.status(status).json({ jsonrpc: "2.0", error: { code, message }, id: null });
}

/**
 * The MCP endpoint, stateless: each POST gets its own server and transport, closed with the
 * response. There are no sessions or server-initiated streams, so GET and DELETE answer 405.
 * Mounted after the access-token check and the JSON body parser.
 */
export function createMcpRouter(db: AppDatabase): Router {
  const router = Router();

  router.post("/", async (req: Request, res: Response) => {
    try {
      const server = createMcpServer(db);
      const transport = new NodeStreamableHTTPServerTransport({ sessionIdGenerator: undefined });
      res.on("close", () => {
        void transport.close();
        void server.close();
      });
      await server.connect(transport);
      await transport.handleRequest(req, res, req.body);
    } catch (err) {
      logger.error("mcp_request_error", { message: err instanceof Error ? err.message : String(err) });
      if (!res.headersSent) jsonRpcError(res, 500, -32603, "Internal server error");
    }
  });

  const notAllowed = (_req: Request, res: Response) => {
    res.setHeader("Allow", "POST");
    jsonRpcError(res, 405, -32000, "Method not allowed: this MCP server is stateless, use POST");
  };
  router.get("/", notAllowed);
  router.delete("/", notAllowed);

  return router;
}
