import { useMemo, useState } from "react";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { createMenu, deleteMenu, fetchMenus, updateMenu } from "./api";
import type { MenuItem } from "./types";

type Props = {
  loginUserName: string;
};

const initialCreateForm = {
  menuId: "",
  parentPk: "",
  depthLevel: 0,
  menuType: "PAGE",
  routePath: "",
  sortOrder: 0,
  isVisible: "Y",
  isEnabled: "Y",
  icon: "",
};

export function MenuAdminPanel({ loginUserName }: Props) {
  const queryClient = useQueryClient();
  const [selected, setSelected] = useState<MenuItem | null>(null);
  const [createForm, setCreateForm] = useState(initialCreateForm);
  const [message, setMessage] = useState<string | null>(null);

  const menusQuery = useQuery({
    queryKey: ["menus"],
    queryFn: fetchMenus,
  });

  const [updateForm, setUpdateForm] = useState({
    parentPk: "",
    depthLevel: 0,
    menuType: "PAGE",
    routePath: "",
    sortOrder: 0,
    isVisible: "Y",
    isEnabled: "Y",
    icon: "",
  });

  const createMutation = useMutation({
    mutationFn: createMenu,
    onSuccess: async () => {
      setMessage("메뉴를 생성했습니다.");
      setCreateForm(initialCreateForm);
      await queryClient.invalidateQueries({ queryKey: ["menus"] });
    },
    onError: () => setMessage("메뉴 생성에 실패했습니다."),
  });

  const updateMutation = useMutation({
    mutationFn: ({ menuPk, payload }: { menuPk: number; payload: Parameters<typeof updateMenu>[1] }) =>
      updateMenu(menuPk, payload),
    onSuccess: async () => {
      setMessage("메뉴를 수정했습니다.");
      await queryClient.invalidateQueries({ queryKey: ["menus"] });
    },
    onError: () => setMessage("메뉴 수정에 실패했습니다."),
  });

  const deleteMutation = useMutation({
    mutationFn: deleteMenu,
    onSuccess: async () => {
      setMessage("메뉴를 삭제했습니다.");
      setSelected(null);
      await queryClient.invalidateQueries({ queryKey: ["menus"] });
    },
    onError: () => setMessage("메뉴 삭제에 실패했습니다. 하위 메뉴가 있으면 삭제할 수 없습니다."),
  });

  const sortedMenus = useMemo(() => {
    return [...(menusQuery.data ?? [])].sort((a, b) => {
      if (a.sortOrder !== b.sortOrder) {
        return a.sortOrder - b.sortOrder;
      }
      return a.menuPk - b.menuPk;
    });
  }, [menusQuery.data]);

  const visibleMenus = useMemo(
    () => sortedMenus.filter((m) => m.isVisible === "Y" && m.isEnabled === "Y"),
    [sortedMenus]
  );

  const selectMenu = (menu: MenuItem) => {
    setSelected(menu);
    setUpdateForm({
      parentPk: menu.parentPk?.toString() ?? "",
      depthLevel: menu.depthLevel,
      menuType: menu.menuType,
      routePath: menu.routePath ?? "",
      sortOrder: menu.sortOrder,
      isVisible: menu.isVisible,
      isEnabled: menu.isEnabled,
      icon: menu.icon ?? "",
    });
  };

  return (
    <section className="rounded-xl border border-slate-200 bg-white p-4">
      <h2 className="mb-3 text-lg font-medium">메뉴 관리 (TB_MENU CRUD)</h2>
      {message && <p className="mb-3 text-sm text-slate-700">{message}</p>}

      <div className="mb-4 rounded border border-slate-200 bg-slate-50 p-3">
        <p className="mb-2 text-sm font-medium text-slate-700">연동된 메뉴 미리보기 (is_visible=Y, is_enabled=Y)</p>
        <div className="flex flex-wrap gap-2">
          {visibleMenus.map((menu) => (
            <span key={menu.menuPk} className="rounded bg-slate-900 px-2 py-1 text-xs text-white">
              {menu.icon ? `${menu.icon} ` : ""}
              {menu.menuId}
              {menu.routePath ? ` (${menu.routePath})` : ""}
            </span>
          ))}
          {visibleMenus.length === 0 && <span className="text-xs text-slate-500">표시 가능한 메뉴가 없습니다.</span>}
        </div>
      </div>

      <div className="mb-4 overflow-auto rounded border border-slate-200">
        <table className="min-w-full text-sm">
          <thead className="bg-slate-50 text-left">
            <tr>
              <th className="px-3 py-2">PK</th>
              <th className="px-3 py-2">MENU_ID</th>
              <th className="px-3 py-2">PARENT_PK</th>
              <th className="px-3 py-2">DEPTH</th>
              <th className="px-3 py-2">TYPE</th>
              <th className="px-3 py-2">ROUTE</th>
              <th className="px-3 py-2">SORT</th>
              <th className="px-3 py-2">VISIBLE</th>
              <th className="px-3 py-2">ENABLED</th>
            </tr>
          </thead>
          <tbody>
            {sortedMenus.map((menu) => (
              <tr
                key={menu.menuPk}
                className={`cursor-pointer border-t border-slate-100 ${selected?.menuPk === menu.menuPk ? "bg-slate-100" : "bg-white"}`}
                onClick={() => selectMenu(menu)}
              >
                <td className="px-3 py-2">{menu.menuPk}</td>
                <td className="px-3 py-2">{menu.menuId}</td>
                <td className="px-3 py-2">{menu.parentPk ?? "-"}</td>
                <td className="px-3 py-2">{menu.depthLevel}</td>
                <td className="px-3 py-2">{menu.menuType}</td>
                <td className="px-3 py-2">{menu.routePath ?? "-"}</td>
                <td className="px-3 py-2">{menu.sortOrder}</td>
                <td className="px-3 py-2">{menu.isVisible}</td>
                <td className="px-3 py-2">{menu.isEnabled}</td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>

      <div className="grid grid-cols-1 gap-4 md:grid-cols-2">
        <form
          className="rounded border border-slate-200 p-3"
          onSubmit={(e) => {
            e.preventDefault();
            createMutation.mutate({
              menuId: createForm.menuId,
              parentPk: createForm.parentPk ? Number(createForm.parentPk) : undefined,
              depthLevel: Number(createForm.depthLevel),
              menuType: createForm.menuType,
              routePath: createForm.routePath || undefined,
              sortOrder: Number(createForm.sortOrder),
              isVisible: createForm.isVisible,
              isEnabled: createForm.isEnabled,
              icon: createForm.icon || undefined,
              createdBy: loginUserName || "admin",
            });
          }}
        >
          <h3 className="mb-2 font-medium">메뉴 생성</h3>
          <div className="grid grid-cols-1 gap-2">
            <input className="rounded border border-slate-300 px-3 py-2" placeholder="MENU_ID" value={createForm.menuId} onChange={(e) => setCreateForm((f) => ({ ...f, menuId: e.target.value }))} />
            <input className="rounded border border-slate-300 px-3 py-2" placeholder="PARENT_PK (선택)" value={createForm.parentPk} onChange={(e) => setCreateForm((f) => ({ ...f, parentPk: e.target.value }))} />
            <input className="rounded border border-slate-300 px-3 py-2" type="number" placeholder="DEPTH_LEVEL" value={createForm.depthLevel} onChange={(e) => setCreateForm((f) => ({ ...f, depthLevel: Number(e.target.value) }))} />
            <input className="rounded border border-slate-300 px-3 py-2" placeholder="MENU_TYPE" value={createForm.menuType} onChange={(e) => setCreateForm((f) => ({ ...f, menuType: e.target.value }))} />
            <input className="rounded border border-slate-300 px-3 py-2" placeholder="ROUTE_PATH" value={createForm.routePath} onChange={(e) => setCreateForm((f) => ({ ...f, routePath: e.target.value }))} />
            <input className="rounded border border-slate-300 px-3 py-2" type="number" placeholder="SORT_ORDER" value={createForm.sortOrder} onChange={(e) => setCreateForm((f) => ({ ...f, sortOrder: Number(e.target.value) }))} />
            <input className="rounded border border-slate-300 px-3 py-2" placeholder="ICON" value={createForm.icon} onChange={(e) => setCreateForm((f) => ({ ...f, icon: e.target.value }))} />
            <div className="grid grid-cols-2 gap-2">
              <select className="rounded border border-slate-300 px-3 py-2" value={createForm.isVisible} onChange={(e) => setCreateForm((f) => ({ ...f, isVisible: e.target.value }))}>
                <option value="Y">VISIBLE: Y</option>
                <option value="N">VISIBLE: N</option>
              </select>
              <select className="rounded border border-slate-300 px-3 py-2" value={createForm.isEnabled} onChange={(e) => setCreateForm((f) => ({ ...f, isEnabled: e.target.value }))}>
                <option value="Y">ENABLED: Y</option>
                <option value="N">ENABLED: N</option>
              </select>
            </div>
            <button className="rounded bg-blue-600 px-4 py-2 text-white" type="submit">생성</button>
          </div>
        </form>

        <form
          className="rounded border border-slate-200 p-3"
          onSubmit={(e) => {
            e.preventDefault();
            if (!selected) return;
            updateMutation.mutate({
              menuPk: selected.menuPk,
              payload: {
                parentPk: updateForm.parentPk ? Number(updateForm.parentPk) : undefined,
                depthLevel: Number(updateForm.depthLevel),
                menuType: updateForm.menuType,
                routePath: updateForm.routePath || undefined,
                sortOrder: Number(updateForm.sortOrder),
                isVisible: updateForm.isVisible,
                isEnabled: updateForm.isEnabled,
                icon: updateForm.icon || undefined,
                updatedBy: loginUserName || "admin",
              },
            });
          }}
        >
          <h3 className="mb-2 font-medium">메뉴 수정/삭제</h3>
          <div className="grid grid-cols-1 gap-2">
            <input className="rounded border border-slate-300 px-3 py-2" readOnly placeholder="MENU_ID" value={selected?.menuId ?? ""} />
            <input className="rounded border border-slate-300 px-3 py-2" placeholder="PARENT_PK (선택)" value={updateForm.parentPk} onChange={(e) => setUpdateForm((f) => ({ ...f, parentPk: e.target.value }))} disabled={!selected} />
            <input className="rounded border border-slate-300 px-3 py-2" type="number" placeholder="DEPTH_LEVEL" value={updateForm.depthLevel} onChange={(e) => setUpdateForm((f) => ({ ...f, depthLevel: Number(e.target.value) }))} disabled={!selected} />
            <input className="rounded border border-slate-300 px-3 py-2" placeholder="MENU_TYPE" value={updateForm.menuType} onChange={(e) => setUpdateForm((f) => ({ ...f, menuType: e.target.value }))} disabled={!selected} />
            <input className="rounded border border-slate-300 px-3 py-2" placeholder="ROUTE_PATH" value={updateForm.routePath} onChange={(e) => setUpdateForm((f) => ({ ...f, routePath: e.target.value }))} disabled={!selected} />
            <input className="rounded border border-slate-300 px-3 py-2" type="number" placeholder="SORT_ORDER" value={updateForm.sortOrder} onChange={(e) => setUpdateForm((f) => ({ ...f, sortOrder: Number(e.target.value) }))} disabled={!selected} />
            <input className="rounded border border-slate-300 px-3 py-2" placeholder="ICON" value={updateForm.icon} onChange={(e) => setUpdateForm((f) => ({ ...f, icon: e.target.value }))} disabled={!selected} />
            <div className="grid grid-cols-2 gap-2">
              <select className="rounded border border-slate-300 px-3 py-2" value={updateForm.isVisible} onChange={(e) => setUpdateForm((f) => ({ ...f, isVisible: e.target.value }))} disabled={!selected}>
                <option value="Y">VISIBLE: Y</option>
                <option value="N">VISIBLE: N</option>
              </select>
              <select className="rounded border border-slate-300 px-3 py-2" value={updateForm.isEnabled} onChange={(e) => setUpdateForm((f) => ({ ...f, isEnabled: e.target.value }))} disabled={!selected}>
                <option value="Y">ENABLED: Y</option>
                <option value="N">ENABLED: N</option>
              </select>
            </div>
            <div className="flex gap-2">
              <button className="rounded bg-emerald-600 px-4 py-2 text-white disabled:opacity-50" type="submit" disabled={!selected}>수정</button>
              <button className="rounded bg-rose-600 px-4 py-2 text-white disabled:opacity-50" type="button" disabled={!selected} onClick={() => selected && deleteMutation.mutate(selected.menuPk)}>삭제</button>
            </div>
          </div>
        </form>
      </div>
    </section>
  );
}
