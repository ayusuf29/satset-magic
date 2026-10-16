import React from 'react';
import { interpolate, spring, useCurrentFrame, useVideoConfig } from 'remotion';
import { THEME } from '../types';

export const Scene6_Outro: React.FC = () => {
  const frame = useCurrentFrame();
  const { fps } = useVideoConfig();

  const scale = spring({ frame, fps, config: { damping: 14 } });
  const opacity = interpolate(frame, [0, 20], [0, 1]);

  return (
    <div
      style={{
        display: 'flex',
        flexDirection: 'column',
        alignItems: 'center',
        justifyContent: 'center',
        height: '100%',
        padding: '60px 100px',
        textAlign: 'center',
        opacity,
      }}
    >
      <div
        style={{
          transform: `scale(${scale})`,
          backgroundColor: 'rgba(16,185,129,0.1)',
          border: '2px solid #10b981',
          borderRadius: '50%',
          width: 120,
          height: 120,
          display: 'flex',
          alignItems: 'center',
          justifyContent: 'center',
          fontSize: 60,
          marginBottom: 24,
          boxShadow: '0 0 50px rgba(16,185,129,0.3)',
        }}
      >
        🚀
      </div>

      <h1
        style={{
          fontSize: 64,
          fontWeight: 900,
          margin: 0,
          background: 'linear-gradient(135deg, #ffffff 0%, #34d399 100%)',
          WebkitBackgroundClip: 'text',
          WebkitTextFillColor: 'transparent',
        }}
      >
        Enterprise High-Trust Autonomous Engineering
      </h1>

      <p style={{ fontSize: 26, color: THEME.textMuted, maxWidth: 850, marginTop: 20, lineHeight: 1.5 }}>
        Kecepatan rekayasa 10x dengan keamanan zero-risk melalui konsensus Triad Council dan otorisasi manusia mutlak.
      </p>

      <div
        style={{
          marginTop: 40,
          display: 'inline-flex',
          alignItems: 'center',
          gap: 12,
          padding: '12px 28px',
          backgroundColor: THEME.surface,
          borderRadius: 30,
          border: `1px solid ${THEME.border}`,
          color: '#cbd5e1',
          fontSize: 18,
          fontWeight: 600,
        }}
      >
        <span>Satset Magic Platform</span>
        <span>•</span>
        <span style={{ color: THEME.primary }}>5-Team Topology</span>
        <span>•</span>
        <span style={{ color: THEME.warning }}>Protected Main Branch</span>
      </div>
    </div>
  );
};
