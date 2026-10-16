import { HttpInterceptorFn } from '@angular/common/http';
import { environment } from '../../environments/environment';

const AUTH_EXCLUDED_PATHS = ['/api/auth/login', '/api/auth/signup'];

export const authInterceptor: HttpInterceptorFn = (request, next) => {
  const isBaseApiRequest = request.url.startsWith(environment.apiUrl);
  const isExcludedAuthRequest = AUTH_EXCLUDED_PATHS.some((path) =>
    request.url.startsWith(`${environment.apiUrl}${path}`)
  );

  if (!isBaseApiRequest || isExcludedAuthRequest) {
    return next(request);
  }

  const token = localStorage.getItem('auth_token');
  if (!token) {
    return next(request);
  }

  return next(
    request.clone({
      setHeaders: {
        Authorization: `Bearer ${token}`,
      },
    })
  );
};