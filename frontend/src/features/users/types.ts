export interface UserAccount {
  seq: number;
  id: string;
  name: string;
  email: string;
  mobileNo: string;
  useYn: string;
  crtBy: string;
  crtDt: string;
  crtIp: string;
  amnBy: string | null;
  amnDt: string | null;
  amnIp: string | null;
}

export interface ApiEnvelope<T> {
  data: T;
}

export interface CreateUserPayload {
  id: string;
  name: string;
  password: string;
  email: string;
  mobileNo: string;
  useYn: string;
  crtBy: string;
  crtIp: string;
}

export interface UpdateUserPayload {
  name: string;
  password?: string;
  email: string;
  mobileNo: string;
  useYn: string;
  amnBy: string;
  amnIp: string;
}
