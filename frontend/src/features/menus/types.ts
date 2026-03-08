export interface MenuItem {
  menuPk: number;
  menuId: string;
  parentPk: number | null;
  depthLevel: number;
  menuType: string;
  routePath: string | null;
  sortOrder: number;
  isVisible: string;
  isEnabled: string;
  icon: string | null;
  createdAt: string;
  createdBy: string;
  updatedAt: string | null;
  updatedBy: string | null;
}

export interface ApiEnvelope<T> {
  data: T;
}

export interface CreateMenuPayload {
  menuId: string;
  parentPk?: number;
  depthLevel: number;
  menuType: string;
  routePath?: string;
  sortOrder: number;
  isVisible: string;
  isEnabled: string;
  icon?: string;
  createdBy: string;
}

export interface UpdateMenuPayload {
  parentPk?: number;
  depthLevel: number;
  menuType: string;
  routePath?: string;
  sortOrder: number;
  isVisible: string;
  isEnabled: string;
  icon?: string;
  updatedBy: string;
}
