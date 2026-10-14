import { NextResponse } from "next/server";
import { getSession } from "@/lib/session";

/**
 * Returns the current user, read from the session cookie.
 *
 * Deliberately not called from the root layout: a Server Component reading cookies()
 * forces the whole route into dynamic (per-request) rendering, which would also pull
 * every static page (/, /converter, ...) along with it just to show the Navbar's auth
 * state. Calling this endpoint from a Client Component after hydration keeps the rest
 * of the app statically prerendered, at the cost of the Navbar showing the signed-out
 * state for one fetch round-trip.
 */
export async function GET() {
  const session = await getSession();
  return NextResponse.json(
    { user: session?.user ?? null },
    { headers: { "Cache-Control": "no-store" } }
  );
}
