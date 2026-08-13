import clsx from "clsx";

interface AvatarProps {
  name: string;
  size?: "sm" | "md" | "lg";
  className?: string;
  title?: string;
}

const SIZE_CLASSES = {
  sm: "h-6 w-6 text-[10px]",
  md: "h-8 w-8 text-xs",
  lg: "h-10 w-10 text-sm",
};

const COLOR_CLASSES = [
  "bg-blue-600 text-white dark:bg-blue-500",
  "bg-emerald-600 text-white dark:bg-emerald-500",
  "bg-indigo-600 text-white dark:bg-indigo-500",
  "bg-purple-600 text-white dark:bg-purple-500",
  "bg-amber-600 text-white dark:bg-amber-500",
  "bg-rose-600 text-white dark:bg-rose-500",
  "bg-teal-600 text-white dark:bg-teal-500",
];

function getColorClass(name: string) {
  let hash = 0;
  for (let i = 0; i < name.length; i++) {
    hash = name.charCodeAt(i) + ((hash << 5) - hash);
  }
  const index = Math.abs(hash) % COLOR_CLASSES.length;
  return COLOR_CLASSES[index];
}

export function Avatar({ name, size = "md", className, title }: AvatarProps) {
  const initials = name
    .trim()
    .split(/\s+/)
    .map((part) => part.charAt(0))
    .join("")
    .toUpperCase()
    .slice(0, 2);

  const colorClass = getColorClass(name);

  return (
    <span
      title={title || name}
      className={clsx(
        "inline-flex items-center justify-center font-medium rounded-full ring-2 ring-white dark:ring-ink-800 flex-shrink-0 select-none shadow-sm",
        SIZE_CLASSES[size],
        colorClass,
        className
      )}
    >
      {initials || "?"}
    </span>
  );
}
