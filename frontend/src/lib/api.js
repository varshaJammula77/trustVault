const BASE = (import.meta.env.VITE_API_URL || '/api').replace(/\/$/, '');

export async function api(path, opts = {}) {
  const token = localStorage.getItem('tv_token');

  const headers = {
    ...(opts.body instanceof FormData
      ? {}
      : { 'Content-Type': 'application/json' }),
    ...(opts.headers || {})
  };

  if (token) {
    headers.Authorization = `Bearer ${token}`;
  }

  const response = await fetch(`${BASE}${path}`, {
    ...opts,
    headers
  });

  if (!response.ok) {
    let payload = {};

    try {
      const text = await response.text();
      if (text) {
        payload = JSON.parse(text);
      }
    } catch {
      // Ignore non-JSON error responses
    }

    throw new Error(
      payload.error || `Request failed (${response.status})`
    );
  }

  // DELETE endpoints may return 204 or 200 with an empty body.
  if (response.status === 204) {
    return null;
  }

  const text = await response.text();

  // Successful response with no body.
  if (!text.trim()) {
    return null;
  }

  // Successful JSON response.
  try {
    return JSON.parse(text);
  } catch {
    throw new Error('Server returned an invalid JSON response');
  }
}

export async function download(id, name) {
  const token = localStorage.getItem('tv_token');

  const response = await fetch(
    `${BASE}/documents/${encodeURIComponent(id)}/download`,
    {
      headers: {
        Authorization: `Bearer ${token}`
      }
    }
  );

  if (!response.ok) {
    let payload = {};

    try {
      const text = await response.text();
      if (text) {
        payload = JSON.parse(text);
      }
    } catch {
      // Ignore non-JSON error responses
    }

    throw new Error(
      payload.error || `Download failed (${response.status})`
    );
  }

  const blob = await response.blob();
  const url = URL.createObjectURL(blob);

  const anchor = document.createElement('a');
  anchor.href = url;
  anchor.download = name;

  document.body.appendChild(anchor);
  anchor.click();
  anchor.remove();

  window.setTimeout(() => URL.revokeObjectURL(url), 1000);
}