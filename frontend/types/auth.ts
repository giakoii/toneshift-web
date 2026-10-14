export type Role = "USER" | "ADMIN";
export type AuthProviderType = "GOOGLE";

export interface UserResponse {
  id: string;
  name: string;
  email: string;
  image: string | null;
  role: Role;
  hasPassword: boolean;
  providers: AuthProviderType[];
  createdAt: string; // ISO-8601
}

export interface AuthResponse {
  accessToken: string;
  tokenType: "Bearer";
  expiresIn: number; // giây, hiện tại luôn 900
  refreshToken: string;
  user: UserResponse;
}

// Khớp vn.toneshift.api.common.dto.ErrorResponse
export interface FieldErrorItem {
  field: string;
  message: string;
}

export interface ApiErrorBody {
  timestamp: string;
  status: number;
  code: string;
  message: string;
  path: string;
  traceId: string;
  errors: FieldErrorItem[];
}
