import logoPng from '../../assets/logo.png';

interface LogoProps {
  size?: number;
  className?: string;
  variant?: 'icon' | 'full';
  theme?: 'light' | 'dark';
}

export function Logo({ size = 48, className = '', variant = 'icon', theme = 'light' }: LogoProps) {
  const isDark = theme === 'dark';

  return (
    <img
      src={logoPng}
      alt="AnkiDeku"
      width={variant === 'full' ? size * 4 : size}
      height={size}
      className={className}
      style={{
        filter: isDark ? 'brightness(1.1)' : 'none',
        objectFit: 'contain'
      }}
    />
  );
}
