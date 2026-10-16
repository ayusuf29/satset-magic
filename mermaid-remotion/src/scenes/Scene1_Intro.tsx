import React from 'react';
import { interpolate, spring, useCurrentFrame, useVideoConfig } from 'remotion';
import { THEME } from '../types';

export const Scene1_Intro: React.FC = () => {
  const frame = useCurrentFrame();
  const { fps } = useVideoConfig();

  const titleProgress = spring({ frame, fps, config: { damping: 14 } });
  const subtitleOpacity = interpolate(frame, [25, 45], [0, 1], { extrapolateRight: 'clamp' });
  const nodesProgress = spring({ frame: frame - 40, fps, config: { damping: 12 } });

  const teams = [
    { title: 'Product & Design', icon: '🎨', desc: 'PO, BA & UI/UX' },
    { title: 'Development Team', icon: '💻', desc: 'Backend & Frontend' },
    { title: 'CI/CD & DevOps', icon: '🚀', desc: 'Docker & Ephemeral' },
    { title: 'Production & SRE', icon: '📊', desc: 'OTel & Metrics' },
    { title: 'Shared Enabling Council', icon: '🏛️', desc: 'Architect, Sec & Perf', highlight: true },
  ];

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
      }}
    >
      <div
        style={{
          transform: `scale(${titleProgress}) translateY(${interpolate(titleProgress, [0, 1], [40, 0])}px)`,
        }}
      >
        <div
          style={{
            fontSize: 20,
            letterSpacing: 4,
            fontWeight: 800,
            color: THEME.primary,
            textTransform: 'uppercase',
            marginBottom: 16,
          }}
        >
          SATSET AUTONOMOUS ENGINEERING PLATFORM
        </div>
        <h1
          style={{
            fontSize: 68,
            fontWeight: 900,
            margin: 0,
            lineHeight: 1.1,
            background: 'linear-gradient(135deg, #ffffff 0%, #cbd5e1 50%, #94a3b8 100%)',
            WebkitBackgroundClip: 'text',
            WebkitTextFillColor: 'transparent',
          }}
        >
          Autonomous Swarm + Human Governance
        </h1>
      </div>

      <p
        style={{
          fontSize: 26,
          color: THEME.textMuted,
          maxWidth: 950,
          marginTop: 24,
          opacity: subtitleOpacity,
          lineHeight: 1.5,
        }}
      >
        Menggabungkan kecepatan rekayasa otonom 5-Team Topology dengan ketegasan gerbang persetujuan manusia
        (Human-in-the-Loop) di setiap titik kritis.
      </p>

      {/* 5-Team Grid */}
      <div
        style={{
          display: 'grid',
          gridTemplateColumns: 'repeat(5, 1fr)',
          gap: 24,
          marginTop: 64,
          width: '100%',
          maxWidth: 1500,
          transform: `scale(${nodesProgress})`,
          opacity: nodesProgress,
        }}
      >
        {teams.map((t, i) => (
          <div
            key={i}
            style={{
              backgroundColor: t.highlight ? 'rgba(99,102,241,0.15)' : THEME.surface,
              border: `2px solid ${t.highlight ? THEME.indigo : THEME.border}`,
              borderRadius: 20,
              padding: 24,
              textAlign: 'center',
              boxShadow: t.highlight ? '0 10px 30px rgba(99,102,241,0.25)' : 'none',
            }}
          >
            <div style={{ fontSize: 44, marginBottom: 12 }}>{t.icon}</div>
            <div style={{ fontSize: 20, fontWeight: 700, color: t.highlight ? '#a5b4fc' : THEME.text }}>
              {t.title}
            </div>
            <div style={{ fontSize: 15, color: THEME.textMuted, marginTop: 6 }}>{t.desc}</div>
          </div>
        ))}
      </div>
    </div>
  );
};
