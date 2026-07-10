/**
 * Type definitions for ToneShift
 */

export interface NavLink {
  label: string;
  href: string;
}

export interface PricingPlan {
  id: string;
  name: string;
  price: number;
  currency: "VND" | "USD";
  description: string;
  features: string[];
  highlighted?: boolean;
}

export interface Testimonial {
  id: string;
  name: string;
  role: string;
  content: string;
  avatar?: string;
}
