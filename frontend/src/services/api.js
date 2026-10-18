const API = import.meta.env.VITE_API_URL || 'http://localhost:8080/api';
const authHeaders = (token) => token
  ? { 'Content-Type': 'application/json', Authorization: `Bearer ${token}` }
  : { 'Content-Type': 'application/json' };
export async function getItems() {
  const response = await fetch(`${API}/items`);
  if (!response.ok) throw new Error('Unable to load listings');
  return response.json();
}
export async function registerStudent(payload) {
  const response = await fetch(`${API}/auth/register`, { method: 'POST', headers: {'Content-Type':'application/json'}, body: JSON.stringify(payload) });
  const data = await response.json().catch(() => ({}));
  if (!response.ok) throw new Error(data.message || 'Registration failed');
  return data;
}
export async function loginStudent(payload) {
  const response = await fetch(`${API}/auth/login`, { method: 'POST', headers: {'Content-Type':'application/json'}, body: JSON.stringify(payload) });
  const data = await response.json().catch(() => ({}));
  if (!response.ok) throw new Error(data.message || 'Sign in failed');
  return data;
}
export async function requestPasswordReset(email) {
  const response = await fetch(`${API}/auth/forgot-password`, { method: 'POST', headers: {'Content-Type':'application/json'}, body: JSON.stringify({email}) });
  const data = await response.json().catch(() => ({}));
  if (!response.ok) throw new Error(data.message || 'Could not request a password reset');
  return data;
}
export async function resetPassword(token, password) {
  const response = await fetch(`${API}/auth/reset-password`, { method: 'POST', headers: {'Content-Type':'application/json'}, body: JSON.stringify({token, password}) });
  const data = await response.json().catch(() => ({}));
  if (!response.ok) throw new Error(data.message || 'Could not reset the password');
  return data;
}
export async function verifyStudentEmail(email, code) {
  const response = await fetch(`${API}/auth/verify`, { method: 'POST', headers: {'Content-Type':'application/json'}, body: JSON.stringify({email, code}) });
  const data = await response.json().catch(() => ({}));
  if (!response.ok) throw new Error(data.message || 'Email verification failed');
  return data;
}
export async function reportItem(payload, token, image) {
  const formData = new FormData();
  formData.append('report', new Blob([JSON.stringify(payload)], { type: 'application/json' }));
  if (image) formData.append('image', image);
  const response = await fetch(`${API}/items`, {
    method: 'POST',
    headers: { Authorization: `Bearer ${token}` },
    body: formData
  });
  const data = await response.json().catch(() => ({}));
  if (!response.ok) throw new Error(data.message || 'Could not submit report');
  return data;
}
export async function submitClaim(payload, token) {
  const response = await fetch(`${API}/claims`, {
    method: 'POST',
    headers: { ...authHeaders(token) },
    body: JSON.stringify(payload)
  });
  const data = await response.json().catch(() => ({}));
  if (!response.ok) {
    const message = response.status === 404
      ? 'The backend is out of date. Stop it with Ctrl+C and restart it with mvn spring-boot:run.'
      : data.message || 'Could not submit the verification details';
    throw new Error(message);
  }
  return data;
}
export async function getSecurityItems(token) {
  const response = await fetch(`${API}/items/security`, { headers: { Authorization: `Bearer ${token}` } });
  const data = await response.json().catch(() => ({}));
  if (!response.ok) {
    const message = response.status === 404
      ? 'The backend is out of date. Stop it with Ctrl+C and restart it from the backend folder.'
      : data.message || `Could not load reports for Campus Security (HTTP ${response.status})`;
    throw new Error(message);
  }
  return data;
}
export async function getPendingClaims(token) {
  const response = await fetch(`${API}/claims/pending`, { headers: { Authorization: 'Bearer ' + token } });
  const data = await response.json().catch(() => ({}));
  if (!response.ok) {
    const message = response.status === 404
      ? 'The backend is out of date. Stop it with Ctrl+C and restart it with mvn spring-boot:run.'
      : data.message || 'Could not load pending claims';
    throw new Error(message);
  }
  return data;
}
export async function reviewClaim(id, decision, token) {
  const response = await fetch(`${API}/claims/${id}/review`, {
    method: 'PATCH',
    headers: authHeaders(token),
    body: JSON.stringify({decision})
  });
  const data = await response.json().catch(() => ({}));
  if (!response.ok) {
    const message = response.status === 404
      ? 'The backend is out of date. Stop it with Ctrl+C and restart it with mvn spring-boot:run.'
      : data.message || 'Could not review submission';
    throw new Error(message);
  }
  return data;
}
export async function getMyItems(token) {
  const response = await fetch(`${API}/items/mine`, { headers: { Authorization: `Bearer ${token}` } });
  const data = await response.json().catch(() => ({}));
  if (!response.ok) throw new Error(data.message || 'Could not load your reports');
  return data;
}
export async function resolveMyItem(id, token) {
  const response = await fetch(`${API}/items/${id}/resolve`, {
    method: 'PATCH',
    headers: { Authorization: `Bearer ${token}` }
  });
  const data = await response.json().catch(() => ({}));
  if (!response.ok) throw new Error(data.message || 'Could not mark report as resolved');
  return data;
}
export async function deleteMyItem(id, token) {
  const response = await fetch(`${API}/items/${id}/mine`, {
    method: 'DELETE',
    headers: { Authorization: `Bearer ${token}` }
  });
  const data = await response.json().catch(() => ({}));
  if (!response.ok) throw new Error(data.message || 'Could not delete report');
}
export async function deleteSecurityItem(id, token) {
  const response = await fetch(`${API}/items/${id}/security`, {
    method: 'DELETE',
    headers: { Authorization: `Bearer ${token}` }
  });
  const data = await response.json().catch(() => ({}));
  if (!response.ok) throw new Error(data.message || 'Could not delete report');
}
export async function emailReportOwner(id, payload, token) {
  const response = await fetch(`${API}/items/${id}/security-email`, {
    method: 'POST',
    headers: authHeaders(token),
    body: JSON.stringify(payload)
  });
  const data = await response.json().catch(() => ({}));
  if (!response.ok) throw new Error(data.message || 'Could not send email to the report owner');
  return data;
}
export async function reviewItem(id, decision, token) {
  const response = await fetch(`${API}/items/${id}/review`, {
    method: 'PATCH',
    headers: authHeaders(token),
    body: JSON.stringify({decision})
  });
  const data = await response.json().catch(() => ({}));
  if (!response.ok) throw new Error(data.message || 'Could not review report');
  return data;
}
export async function logoutStudent(token) {
  await fetch(`${API}/auth/logout`, { method: 'POST', headers: { Authorization: `Bearer ${token}` } });
}