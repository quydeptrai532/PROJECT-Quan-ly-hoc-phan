import { useEffect } from "react";
import { createPortal } from "react-dom";
import { AlertCircle, CheckCircle2, Info, Loader2, X } from "lucide-react";

const BADGE_TONES = {
  slate: "bg-slate-100 text-slate-700 ring-slate-200/80",
  indigo: "bg-slate-900/5 text-slate-900 ring-slate-900/15",
  green: "bg-teal-50 text-teal-800 ring-teal-200/80",
  red: "bg-rose-50 text-rose-700 ring-rose-200/80",
  amber: "bg-amber-50 text-amber-800 ring-amber-300/70",
  sky: "bg-cyan-50 text-cyan-800 ring-cyan-200/80",
};

export function Badge({ tone = "slate", children, className = "", dot = true }) {
  return (
    <span
      className={`inline-flex items-center gap-1.5 rounded-full px-2.5 py-1 text-xs font-medium ring-1 ring-inset ${BADGE_TONES[tone] || BADGE_TONES.slate} ${className}`}
    >
      {dot && <span className="h-1.5 w-1.5 rounded-full bg-current opacity-75" />}
      {children}
    </span>
  );
}

const ALERT_TONES = {
  info: {
    shell: "border-cyan-200/80 bg-cyan-50/70 text-cyan-950",
    icon: "bg-cyan-100 text-cyan-700",
    Icon: Info,
  },
  success: {
    shell: "border-teal-200/80 bg-teal-50/75 text-teal-950",
    icon: "bg-teal-100 text-teal-700",
    Icon: CheckCircle2,
  },
  error: {
    shell: "border-rose-200/80 bg-rose-50/80 text-rose-950",
    icon: "bg-rose-100 text-rose-700",
    Icon: AlertCircle,
  },
  warning: {
    shell: "border-amber-300/70 bg-amber-50/80 text-amber-950",
    icon: "bg-amber-100 text-amber-700",
    Icon: AlertCircle,
  },
};

export function Alert({ tone = "info", children, onClose, className = "" }) {
  if (!children) return null;
  const config = ALERT_TONES[tone] || ALERT_TONES.info;
  const Icon = config.Icon;
  return (
    <div
      role="alert"
      aria-live="polite"
      className={`flex items-start gap-3 rounded-2xl border px-4 py-3.5 text-sm shadow-sm backdrop-blur-sm ${config.shell} ${className}`}
    >
      <span className={`grid h-7 w-7 shrink-0 place-items-center rounded-xl ${config.icon}`}>
        <Icon size={16} />
      </span>
      <div className="flex-1 whitespace-pre-line pt-0.5 leading-relaxed">{children}</div>
      {onClose && (
        <button
          type="button"
          onClick={onClose}
          className="shrink-0 rounded-lg p-1 opacity-55 transition hover:bg-white/70 hover:opacity-100"
          aria-label="Đóng"
        >
          <X size={15} />
        </button>
      )}
    </div>
  );
}

export function Spinner({ label = "Đang tải dữ liệu..." }) {
  return (
    <div className="flex flex-col items-center justify-center gap-3 py-16 text-sm text-slate-500">
      <span className="grid h-11 w-11 place-items-center rounded-2xl bg-[#0b1120] text-amber-400 shadow-md">
        <Loader2 size={20} className="animate-spin" />
      </span>
      <span className="font-medium tracking-wide">{label}</span>
    </div>
  );
}

export function EmptyState({ icon: Icon, title, description, action }) {
  return (
    <div className="flex flex-col items-center justify-center gap-4 px-6 py-16 text-center">
      {Icon && (
        <span className="grid h-14 w-14 place-items-center rounded-2xl bg-gradient-to-br from-[#0b1120] to-[#1e293b] text-amber-400 shadow-lg ring-1 ring-amber-400/20">
          <Icon size={25} />
        </span>
      )}
      <div>
        <p className="font-display text-base font-bold text-slate-900">{title}</p>
        {description && <p className="mx-auto mt-1.5 max-w-sm text-sm leading-relaxed text-slate-500">{description}</p>}
      </div>
      {action}
    </div>
  );
}

export function Modal({ open, onClose, title, description, children, footer, size = "md" }) {
  useEffect(() => {
    if (!open) return undefined;
    const handleKey = (event) => {
      if (event.key === "Escape") onClose();
    };
    document.addEventListener("keydown", handleKey);
    document.body.style.overflow = "hidden";
    return () => {
      document.removeEventListener("keydown", handleKey);
      document.body.style.overflow = "";
    };
  }, [open, onClose]);

  if (!open) return null;

  const sizes = {
    sm: "max-w-md",
    md: "max-w-xl",
    lg: "max-w-3xl",
    xl: "max-w-5xl",
  };

  return createPortal(
    <div className="fixed inset-0 z-50 overflow-y-auto bg-slate-950/60 p-4 backdrop-blur-sm">
      <div className="absolute inset-0" onClick={onClose} aria-hidden="true" />
      <div
        role="dialog"
        aria-modal="true"
        aria-label={title}
        className={`modal-panel relative mx-auto my-8 w-full ${sizes[size]} overflow-hidden rounded-[22px] border border-slate-200/80 bg-white shadow-2xl`}
      >
        <div className="h-1.5 w-full bg-gradient-to-r from-[#0b1120] via-amber-500 to-cyan-500" />
        <div className="flex items-start justify-between gap-4 border-b border-slate-100 px-6 py-4">
          <div>
            <h2 className="font-display text-base font-bold text-slate-900">{title}</h2>
            {description && <p className="mt-1 text-sm leading-relaxed text-slate-500">{description}</p>}
          </div>
          <button
            type="button"
            onClick={onClose}
            className="rounded-xl p-1.5 text-slate-400 transition hover:bg-slate-100 hover:text-slate-700"
            aria-label="Đóng"
          >
            <X size={18} />
          </button>
        </div>
        <div className="px-6 py-5">{children}</div>
        {footer && (
          <div className="flex justify-end gap-2 border-t border-slate-100 bg-[#f7f6f2]/80 px-6 py-4">{footer}</div>
        )}
      </div>
    </div>,
    document.body
  );
}

export function PageHeader({ title, description, actions, icon: Icon, eyebrow }) {
  return (
    <div className="page-enter mb-7 flex flex-col gap-4 border-b border-slate-900/5 pb-5 lg:flex-row lg:items-center lg:justify-between">
      <div className="flex items-start gap-4">
        {Icon ? (
          <span className="mt-0.5 grid h-12 w-12 shrink-0 place-items-center rounded-2xl bg-[#0b1120] text-amber-400 shadow-md ring-1 ring-amber-400/25">
            <Icon size={22} />
          </span>
        ) : (
          <span className="mt-1.5 hidden h-9 w-1.5 rounded-full bg-gradient-to-b from-amber-500 to-cyan-500 sm:block" />
        )}
        <div>
          {eyebrow && (
            <p className="font-mono text-[11px] font-semibold uppercase tracking-[0.2em] text-amber-700">{eyebrow}</p>
          )}
          <h1 className="page-heading text-xl font-bold tracking-tight sm:text-2xl">{title}</h1>
          {description && (
            <p className="mt-1 max-w-2xl text-sm leading-relaxed text-slate-500">{description}</p>
          )}
        </div>
      </div>
      {actions && <div className="flex flex-wrap items-center gap-2.5 lg:justify-end">{actions}</div>}
    </div>
  );
}

const STAT_TONES = {
  indigo: "text-amber-400 bg-[#0b1120] ring-1 ring-amber-400/20",
  emerald: "text-teal-700 bg-teal-50 ring-1 ring-teal-200/80",
  amber: "text-amber-700 bg-amber-50 ring-1 ring-amber-200/80",
  sky: "text-cyan-700 bg-cyan-50 ring-1 ring-cyan-200/80",
  rose: "text-rose-700 bg-rose-50 ring-1 ring-rose-200/80",
};

export function StatCard({ label, value, hint, icon: Icon, tone = "indigo" }) {
  return (
    <div className="stat-card relative overflow-hidden rounded-[20px] p-5">
      <div className="pointer-events-none absolute -right-10 -top-10 h-28 w-28 rounded-full bg-amber-500/[0.04]" />
      <div className="pointer-events-none absolute inset-x-0 top-0 h-[2px] bg-gradient-to-r from-transparent via-amber-400/40 to-transparent" />
      <div className="relative flex items-start justify-between gap-3">
        <div className="min-w-0">
          <p className="stat-label font-mono text-[10.5px] font-semibold uppercase tracking-[0.14em]">{label}</p>
          <p className="mt-2.5 truncate font-display text-3xl font-bold tabular-nums tracking-tight text-slate-950">
            {value}
          </p>
          {hint && <p className="mt-1.5 text-xs leading-snug text-slate-500">{hint}</p>}
        </div>
        {Icon && (
          <span className={`grid h-11 w-11 shrink-0 place-items-center rounded-2xl ${STAT_TONES[tone] || STAT_TONES.indigo}`}>
            <Icon size={21} />
          </span>
        )}
      </div>
    </div>
  );
}

export function Field({ label, required, hint, children }) {
  return (
    <label className="block">
      <span className="label">
        {label} {required && <span className="text-rose-500">*</span>}
      </span>
      {children}
      {hint && <span className="mt-1.5 block text-xs leading-relaxed text-slate-400">{hint}</span>}
    </label>
  );
}
