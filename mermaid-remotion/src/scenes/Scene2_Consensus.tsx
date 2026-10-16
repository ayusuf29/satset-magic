import React from 'react';
import { interpolate, spring, useCurrentFrame, useVideoConfig } from 'remotion';
import { THEME } from '../types';

export const Scene2_Consensus: React.FC = () => {
  const frame = useCurrentFrame();
  const { fps } = useVideoConfig();

  const titleFade = interpolate(frame, [0, 20], [0, 1]);
  const formulaScale = spring({ frame: frame - 20, fps, config: { damping: 12 } });

  // Staggered votes
  const card1 = spring({ frame: frame - 40, fps, config: { damping: 12 } });
  const card2 = spring({ frame: frame - 70, fps, config: { damping: 12 } });
  const card3 = spring({ frame: frame - 100, fps, config: { damping: 12 } });

  const isCard1Passed = frame > 60;
  const isCard2Passed = frame > 90;
  const isCard3Passed = frame > 120;
  const isConsensusPassed = frame > 150;

  return (
    <div
      style={{
        display: 'flex',
        flexDirection: 'column',
        alignItems: 'center',
        justifyContent: 'center',
        height: '100%',
        padding: '50px 80px',
        textAlign: 'center',
      }}
    >
      <div style={{ opacity: titleFade }}>
        <div style={{ fontSize: 18, letterSpacing: 3, fontWeight: 700, color: THEME.indigo }}>
          DEEP COUNCIL CROSS-CUTTING AUDIT
        </div>
        <h2 style={{ fontSize: 54, fontWeight: 900, margin: '8px 0 16px 0', color: THEME.text }}>
          Shared Enabling Team Consensus
        </h2>
      </div>

      {/* Mathematical Consensus Formula Banner */}
      <div
        style={{
          transform: `scale(${formulaScale})`,
          backgroundColor: 'rgba(30,41,59,0.7)',
          border: '1px solid rgba(148,163,184,0.2)',
          borderRadius: 16,
          padding: '12px 32px',
          fontFamily: 'monospace',
          fontSize: 22,
          color: '#cbd5e1',
          marginBottom: 48,
        }}
      >
        Consensus = <span style={{ color: '#60a5fa' }}>Architect</span> ∧{' '}
        <span style={{ color: '#818cf8' }}>Security</span> ∧{' '}
        <span style={{ color: '#34d399' }}>Performance</span> ∧{' '}
        <span style={{ color: '#f59e0b' }}>Coverage ≥ 70%</span>
      </div>

      {/* 3 Council Pillars Cards */}
      <div
        style={{
          display: 'grid',
          gridTemplateColumns: 'repeat(3, 1fr)',
          gap: 32,
          width: '100%',
          maxWidth: 1400,
        }}
      >
        {/* Architect */}
        <div
          style={{
            transform: `scale(${card1})`,
            backgroundColor: THEME.surface,
            border: `2px solid ${isCard1Passed ? THEME.primary : THEME.border}`,
            borderRadius: 24,
            padding: 36,
            textAlign: 'left',
          }}
        >
          <div style={{ fontSize: 44, marginBottom: 12 }}>🏛️</div>
          <div style={{ fontSize: 26, fontWeight: 800, color: '#60a5fa' }}>Enterprise Architect</div>
          <div style={{ fontSize: 16, color: THEME.textMuted, marginTop: 8, minHeight: 48 }}>
            Hexagonal Ports & Adapters, Domain Boundary, & Zero Leaky Abstraction.
          </div>
          <div
            style={{
              marginTop: 24,
              padding: '10px 18px',
              backgroundColor: isCard1Passed ? 'rgba(16,185,129,0.15)' : 'rgba(148,163,184,0.1)',
              color: isCard1Passed ? THEME.success : THEME.textMuted,
              border: `1px solid ${isCard1Passed ? THEME.success : THEME.border}`,
              borderRadius: 12,
              fontWeight: 700,
              display: 'inline-block',
            }}
          >
            {isCard1Passed ? '✓ PASS: Architectural Purity' : '⌛ Evaluating Boundaries...'}
          </div>
        </div>

        {/* Security */}
        <div
          style={{
            transform: `scale(${card2})`,
            backgroundColor: THEME.surface,
            border: `2px solid ${isCard2Passed ? THEME.indigo : THEME.border}`,
            borderRadius: 24,
            padding: 36,
            textAlign: 'left',
          }}
        >
          <div style={{ fontSize: 44, marginBottom: 12 }}>🛡️</div>
          <div style={{ fontSize: 26, fontWeight: 800, color: '#818cf8' }}>AppSec Specialist</div>
          <div style={{ fontSize: 16, color: THEME.textMuted, marginTop: 8, minHeight: 48 }}>
            STRIDE Threat Model, 0 High/Critical SAST, Zero Secret Leakage.
          </div>
          <div
            style={{
              marginTop: 24,
              padding: '10px 18px',
              backgroundColor: isCard2Passed ? 'rgba(16,185,129,0.15)' : 'rgba(148,163,184,0.1)',
              color: isCard2Passed ? THEME.success : THEME.textMuted,
              border: `1px solid ${isCard2Passed ? THEME.success : THEME.border}`,
              borderRadius: 12,
              fontWeight: 700,
              display: 'inline-block',
            }}
          >
            {isCard2Passed ? '✓ PASS: 0 SAST Vulnerability' : '⌛ Scanning AST & Secrets...'}
          </div>
        </div>

        {/* Performance */}
        <div
          style={{
            transform: `scale(${card3})`,
            backgroundColor: THEME.surface,
            border: `2px solid ${isCard3Passed ? THEME.success : THEME.border}`,
            borderRadius: 24,
            padding: 36,
            textAlign: 'left',
          }}
        >
          <div style={{ fontSize: 44, marginBottom: 12 }}>⚡</div>
          <div style={{ fontSize: 26, fontWeight: 800, color: '#34d399' }}>Perf & Database</div>
          <div style={{ fontSize: 16, color: THEME.textMuted, marginTop: 8, minHeight: 48 }}>
            Latency Budget P95 &lt; 200ms, Proper Indexing, 88.4% Test Coverage.
          </div>
          <div
            style={{
              marginTop: 24,
              padding: '10px 18px',
              backgroundColor: isCard3Passed ? 'rgba(16,185,129,0.15)' : 'rgba(148,163,184,0.1)',
              color: isCard3Passed ? THEME.success : THEME.textMuted,
              border: `1px solid ${isCard3Passed ? THEME.success : THEME.border}`,
              borderRadius: 12,
              fontWeight: 700,
              display: 'inline-block',
            }}
          >
            {isCard3Passed ? '✓ PASS: Coverage 88.4% (≥ 70%)' : '⌛ Benchmarking Query Footprint...'}
          </div>
        </div>
      </div>

      {/* Consensus Result Pill */}
      {isConsensusPassed && (
        <div
          style={{
            marginTop: 40,
            padding: '16px 36px',
            backgroundColor: 'rgba(16,185,129,0.2)',
            border: '2px solid #10b981',
            borderRadius: 40,
            color: '#6ee7b7',
            fontSize: 24,
            fontWeight: 800,
            letterSpacing: 1,
            boxShadow: '0 10px 30px rgba(16,185,129,0.3)',
          }}
        >
          🎉 UNANIMOUS TRIAD CONSENSUS REACHED (3/3 VOTES)
        </div>
      )}
    </div>
  );
};
