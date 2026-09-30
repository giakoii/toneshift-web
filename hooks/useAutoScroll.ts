"use client";

import { useCallback, useEffect, useRef, useState, type RefObject } from "react";

export const MIN_SPEED = 0.5;
export const MAX_SPEED = 3;
export const SPEED_STEP = 0.25;

// Speed 1x scrolls 30px per second
const PIXELS_PER_SECOND = 30;
const SPEED_STORAGE_KEY = "toneshift-scroll-speed";

function readStoredSpeed(): number {
  try {
    const raw = window.localStorage.getItem(SPEED_STORAGE_KEY);
    const value = raw ? Number(raw) : 1;
    return Number.isFinite(value) ? clampSpeed(value) : 1;
  } catch {
    return 1;
  }
}

function clampSpeed(value: number): number {
  return Math.min(MAX_SPEED, Math.max(MIN_SPEED, Math.round(value / SPEED_STEP) * SPEED_STEP));
}

/**
 * Smoothly scrolls a container with requestAnimationFrame.
 * Pauses when the user scrolls up by hand and stops at the bottom.
 * Must only be used in components mounted on the client (reads localStorage on init).
 */
export function useAutoScroll(containerRef: RefObject<HTMLElement | null>) {
  const [isPlaying, setIsPlaying] = useState(false);
  const [speed, setSpeedState] = useState(readStoredSpeed);

  // Refs change every frame; keeping them out of state avoids re-rendering at 60fps
  const animationRef = useRef<number | null>(null);
  const lastTimeRef = useRef<number | null>(null);
  // Sub-pixel accumulator: scrollTop only accepts whole pixels in some browsers
  const offsetRef = useRef(0);
  const speedRef = useRef(speed);

  const setSpeed = useCallback((next: number | ((prev: number) => number)) => {
    setSpeedState((prev) => {
      const value = clampSpeed(typeof next === "function" ? next(prev) : next);
      speedRef.current = value;
      try {
        window.localStorage.setItem(SPEED_STORAGE_KEY, String(value));
      } catch {
        // Storage may be unavailable (private mode); speed still works for this session
      }
      return value;
    });
  }, []);

  const toggle = useCallback(() => setIsPlaying((p) => !p), []);
  const pause = useCallback(() => setIsPlaying(false), []);

  useEffect(() => {
    const el = containerRef.current;
    if (!isPlaying || !el) return;

    offsetRef.current = el.scrollTop;
    lastTimeRef.current = null;

    const step = (time: number) => {
      if (lastTimeRef.current !== null) {
        const deltaSeconds = (time - lastTimeRef.current) / 1000;
        offsetRef.current += PIXELS_PER_SECOND * speedRef.current * deltaSeconds;
        el.scrollTop = offsetRef.current;

        if (el.scrollTop + el.clientHeight >= el.scrollHeight - 1) {
          setIsPlaying(false);
          return;
        }
      }
      lastTimeRef.current = time;
      animationRef.current = requestAnimationFrame(step);
    };
    animationRef.current = requestAnimationFrame(step);

    // Scrolling up by hand means the player wants to re-read: pause
    const handleWheel = (e: WheelEvent) => {
      if (e.deltaY < 0) setIsPlaying(false);
    };
    let touchStartY = 0;
    const handleTouchStart = (e: TouchEvent) => {
      touchStartY = e.touches[0]?.clientY ?? 0;
    };
    const handleTouchMove = (e: TouchEvent) => {
      if ((e.touches[0]?.clientY ?? 0) > touchStartY + 10) setIsPlaying(false);
    };

    el.addEventListener("wheel", handleWheel, { passive: true });
    el.addEventListener("touchstart", handleTouchStart, { passive: true });
    el.addEventListener("touchmove", handleTouchMove, { passive: true });

    return () => {
      if (animationRef.current !== null) cancelAnimationFrame(animationRef.current);
      animationRef.current = null;
      el.removeEventListener("wheel", handleWheel);
      el.removeEventListener("touchstart", handleTouchStart);
      el.removeEventListener("touchmove", handleTouchMove);
    };
  }, [isPlaying, containerRef]);

  return { isPlaying, toggle, pause, speed, setSpeed };
}
