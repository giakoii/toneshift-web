// Override 'body' to allow passing JS Objects directly without TypeScript complaining
type CustomOptions = Omit<RequestInit, 'body'> & {
    baseUrl?: string;
    body?: unknown;
}

/**
 * Custom error class to handle HTTP exceptions gracefully.
 * It stores both the HTTP status code and the parsed error payload from the backend.
 */
export class HttpError extends Error {
    status: number;
    payload: unknown;

    constructor(status: number, payload: unknown) {
        const message = (payload as { message?: string })?.message || 'An error occurred during the HTTP request';
        super(message);

        this.status = status;
        this.payload = payload;
        this.name = 'HttpError'; // Good practice for custom Error classes
    }
}

/**
 * A centralized fetch wrapper for making HTTP requests.
 * @param method - The HTTP method (GET, POST, PUT, DELETE, PATCH)
 * @param url - The endpoint URL (can be a relative path or an absolute URL)
 * @param options - Additional fetch options, custom headers, or base URL overrides
 * @returns An object containing the HTTP status and the parsed response payload
 */
export const request = async <Response>(
    method: 'GET' | 'POST' | 'PUT' | 'DELETE' | 'PATCH',
    url: string,
    options?: CustomOptions
) => {
    // Safely stringify the payload object if it exists
    const body = options?.body ? JSON.stringify(options.body) : undefined;

    // Set default headers
    const baseHeaders = { 'Content-Type': 'application/json' };

    // Determine the base URL (fallback to environment variables)
    const baseUrl = options?.baseUrl === undefined ? process.env.API_URL : options.baseUrl;

    // Safely construct the full URL
    const fullUrl = url.startsWith('http')
        ? url
        : url.startsWith('/')
            ? `${baseUrl}${url}`
            : `${baseUrl}/${url}`;

    // Execute the fetch call
    const response = await fetch(fullUrl, {
        ...options,
        headers: {
            ...baseHeaders,
            ...options?.headers,
        },
        method,
        body,
    });

    // Safely parse the JSON response
    let payload: Response | unknown;
    try {
        payload = await response.json();
    } catch {
        payload = {};
    }

    const data = {
        status: response.status,
        payload: payload as Response,
    };

    // 7. Handle HTTP errors (status codes outside 200-299)
    if (!response.ok) {
        // Pass the parsed payload to HttpError so the caller can read backend error messages
        throw new HttpError(response.status, payload);
    }

    return data;
}