import { createContext, useCallback, useContext, useState } from "react";
import { AlertTriangle, HelpCircle, ShieldAlert, Trash2 } from "lucide-react";
import { Modal } from "./ui";

const ConfirmContext = createContext(null);

const TONES = {
  danger: {
    iconTile: "bg-rose-50 text-rose-600 ring-rose-100",
    Icon: Trash2,
    button: "btn-danger",
  },
  warning: {
    iconTile: "bg-amber-50 text-amber-600 ring-amber-100",
    Icon: AlertTriangle,
    button: "btn-warning",
  },
  primary: {
    iconTile: "bg-indigo-50 text-indigo-600 ring-indigo-100",
    Icon: HelpCircle,
    button: "btn-primary",
  },
  security: {
    iconTile: "bg-rose-50 text-rose-600 ring-rose-100",
    Icon: ShieldAlert,
    button: "btn-danger",
  },
};

export function ConfirmProvider({ children }) {
  const [request, setRequest] = useState(null);

  const confirm = useCallback(
    (options) =>
      new Promise((resolve) => {
        setRequest({
          options: typeof options === "string" ? { message: options } : options,
          resolve,
        });
      }),
    []
  );

  const close = (result) => {
    if (!request) return;
    request.resolve(result);
    setRequest(null);
  };

  const options = request?.options || {};
  const tone = TONES[options.tone] || TONES.danger;
  const Icon = tone.Icon;

  return (
    <ConfirmContext.Provider value={confirm}>
      {children}
      <Modal
        open={Boolean(request)}
        onClose={() => close(false)}
        size="sm"
        title={options.title || "Xác nhận thao tác"}
        footer={
          <>
            <button type="button" className="btn-secondary" onClick={() => close(false)}>
              {options.cancelLabel || options.cancelText || "Hủy"}
            </button>
            <button type="button" className={tone.button} onClick={() => close(true)}>
              {options.confirmLabel || options.confirmText || "Xác nhận"}
            </button>
          </>
        }
      >
        <div className="flex gap-4">
          <span className={`grid h-12 w-12 shrink-0 place-items-center rounded-xl ring-1 ${tone.iconTile}`}>
            <Icon size={23} />
          </span>
          <div className="pt-0.5 text-sm leading-relaxed text-slate-600">
            <p className="font-medium text-slate-800">{options.message}</p>
            {options.detail && <p className="mt-2 text-xs leading-relaxed text-slate-500">{options.detail}</p>}
          </div>
        </div>
      </Modal>
    </ConfirmContext.Provider>
  );
}

export function useConfirm() {
  const confirm = useContext(ConfirmContext);
  if (!confirm) {
    throw new Error("useConfirm phải được dùng bên trong ConfirmProvider");
  }
  return confirm;
}
