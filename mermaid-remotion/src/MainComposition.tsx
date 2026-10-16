import React from 'react';
import { Sequence } from 'remotion';
import { VIDEO_CONFIG, THEME } from './types';
import { Scene1_Intro } from './scenes/Scene1_Intro';
import { Scene2_Consensus } from './scenes/Scene2_Consensus';
import { Scene3_BranchProtection } from './scenes/Scene3_BranchProtection';
import { Scene4_TelegramHITL } from './scenes/Scene4_TelegramHITL';
import { Scene5_MergeAndRelease } from './scenes/Scene5_MergeAndRelease';
import { Scene6_Outro } from './scenes/Scene6_Outro';

export const MainComposition: React.FC = () => {
  const { scenes } = VIDEO_CONFIG;

  return (
    <div
      style={{
        flex: 1,
        width: '100%',
        height: '100%',
        backgroundColor: THEME.bg,
        color: THEME.text,
        fontFamily: 'Inter, system-ui, -apple-system, sans-serif',
        position: 'relative',
        overflow: 'hidden',
      }}
    >
      {/* Background Ambient Glows */}
      <div
        style={{
          position: 'absolute',
          top: -200,
          left: -200,
          width: 800,
          height: 800,
          borderRadius: '50%',
          background: 'radial-gradient(circle, rgba(59,130,246,0.15) 0%, rgba(9,13,22,0) 70%)',
          pointerEvents: 'none',
        }}
      />
      <div
        style={{
          position: 'absolute',
          bottom: -200,
          right: -200,
          width: 900,
          height: 900,
          borderRadius: '50%',
          background: 'radial-gradient(circle, rgba(99,102,241,0.12) 0%, rgba(9,13,22,0) 70%)',
          pointerEvents: 'none',
        }}
      />

      {/* Scene 1: Introduction & 5-Team Swarm */}
      <Sequence from={scenes.intro.from} durationInFrames={scenes.intro.duration}>
        <Scene1_Intro />
      </Sequence>

      {/* Scene 2: Triad Council Shared Consensus */}
      <Sequence from={scenes.consensus.from} durationInFrames={scenes.consensus.duration}>
        <Scene2_Consensus />
      </Sequence>

      {/* Scene 3: Branch Protection & Missing HITL Resolved */}
      <Sequence from={scenes.protection.from} durationInFrames={scenes.protection.duration}>
        <Scene3_BranchProtection />
      </Sequence>

      {/* Scene 4: Mobile Telegram HITL Interactive Flow */}
      <Sequence from={scenes.telegram.from} durationInFrames={scenes.telegram.duration}>
        <Scene4_TelegramHITL />
      </Sequence>

      {/* Scene 5: Protected Merge & Pre-Prod Gate */}
      <Sequence from={scenes.merge.from} durationInFrames={scenes.merge.duration}>
        <Scene5_MergeAndRelease />
      </Sequence>

      {/* Scene 6: Outro & Governance Summary */}
      <Sequence from={scenes.outro.from} durationInFrames={scenes.outro.duration}>
        <Scene6_Outro />
      </Sequence>
    </div>
  );
};
