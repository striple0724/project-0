export type ClientType = "INDIVIDUAL" | "CORPORATE";
export type ClientTier = "BASIC" | "PREMIUM" | "VIP";
export type ClientStatus = "ACTIVE" | "INACTIVE";

export interface Client {
  id: number;
  name: string;
  bizNo?: string;
  type: ClientType;
  status: ClientStatus;
  tier: ClientTier;
  createdAt: string;
  updatedAt?: string;
}

export interface ClientFilters {
  name?: string;
  type?: ClientType;
  tier?: ClientTier;
  status?: ClientStatus;
  createdAtFrom?: string;
  createdAtTo?: string;
  page?: number;
  size?: number;
}

export interface PagedClients {
  data: Client[];
  page: {
    size: number;
    number: number;
    totalElements: number;
    totalPages: number;
  };
}
