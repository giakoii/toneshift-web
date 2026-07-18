import React from 'react';
import Link from "next/link";
import Spinner from "@/components/ui/Spinner";

export interface ButtonProps extends React.ButtonHTMLAttributes<HTMLButtonElement> {
    variant?: "primary" | "secondary" | "outline" | "ghost";
    size?: "xs" | "sm" | "md" | "lg" | "xl" | "icon";
    isLoading?: boolean;
    leftIcon?: React.ReactNode;
    rightIcon?: React.ReactNode;
    href?: string;
}

const Button = ({
    children,
    variant = "primary",
    size = "md",
    isLoading = false,
    leftIcon,
    rightIcon,
    href,
    className = "",
    disabled,
    ...props
}: ButtonProps) => {
    const baseClasses = "inline-flex items-center justify-center font-medium rounded-xl transition-all duration-200 active:scale-95 disabled:opacity-50 disabled:pointer-events-none";

    const variantClasses = {
        primary: "bg-amber-600 text-white hover:bg-amber-700 shadow-md",
        secondary: "bg-white/10 text-white hover:bg-white/20",
        outline: "border border-white/20 text-white hover:bg-white/10",
        ghost: "text-white/70 hover:text-white hover:bg-white/10"
    };

    const sizeClasses = {
        xs: "px-2 py-1 text-xs",
        sm: "px-3 py-1.5 text-sm",
        md: "px-4 py-2 text-sm",
        lg: "px-6 py-3 text-base",
        xl: "px-8 py-4 text-lg",
        icon: "w-10 h-10"
    };

    const combinedClassName = `${baseClasses} ${variantClasses[variant]} ${sizeClasses[size]} ${className}`;

    const content = (
        <>
            {isLoading ? <Spinner size="sm" className="mr-2" /> : leftIcon && <span className="mr-2">{leftIcon}</span>}
            {children}
            {!isLoading && rightIcon && <span className="ml-2">{rightIcon}</span>}
        </>
    );

    if (href) {
        return (
            <Link href={href} className={combinedClassName} onClick={props.onClick as any}>
                {content}
            </Link>
        );
    }
    return (
        <button
            className={combinedClassName}
            disabled={disabled || isLoading}
            {...props}
        >
            {content}
        </button>
    );
};

export default Button;