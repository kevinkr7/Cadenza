import { Canvas, useFrame, useThree } from "@react-three/fiber";
import { Environment, Float, Lightformer, RoundedBox, MeshTransmissionMaterial, Sparkles, ContactShadows } from "@react-three/drei";
import { useMemo, useRef } from "react";
import * as THREE from "three";

const PURPLE = "#7c46e8";
const BLUE = "#3f6cf5";
const CYAN = "#4cc4f0";
const SOFT = "#efeaff";

function useReducedMotion() {
  return typeof window !== "undefined"
    ? window.matchMedia("(prefers-reduced-motion: reduce)").matches
    : false;
}

/** 
 * PREMIUM MICROPHONE
 * A sleek, high-end studio microphone. Brushed titanium body with a glowing energy core 
 * encased in a refractive glass capsule. Orbiting neon rings represent vocal capture.
 */
function Microphone({ compact }: { compact: boolean }) {
  const group = useRef<THREE.Group>(null);
  const headRef = useRef<THREE.Mesh>(null);
  const ringRef1 = useRef<THREE.Mesh>(null);
  const ringRef2 = useRef<THREE.Mesh>(null);

  useFrame((state, delta) => {
    if (!group.current) return;
    const dt = Math.min(delta, 0.05);
    group.current.rotation.y += dt * 0.15;
    group.current.position.y = Math.sin(state.clock.elapsedTime * 1.2) * 0.1;
    
    if (headRef.current) {
      headRef.current.rotation.y = state.clock.elapsedTime * -0.3;
    }
    if (ringRef1.current && ringRef2.current) {
      ringRef1.current.rotation.x = Math.sin(state.clock.elapsedTime * 1.5) * 0.15;
      ringRef1.current.rotation.y = state.clock.elapsedTime * 0.5;
      ringRef2.current.rotation.x = Math.cos(state.clock.elapsedTime * 1.2) * 0.15;
      ringRef2.current.rotation.y = state.clock.elapsedTime * -0.4;
    }
  });

  return (
    <group ref={group} scale={compact ? 0.78 : 1} rotation={[0.1, 0, 0.15]}>
      {/* Mic Head - Inner Glowing Energy */}
      <mesh position={[0, 0.9, 0]}>
        <capsuleGeometry args={[0.25, 0.55, 32, 32]} />
        <meshStandardMaterial color={PURPLE} emissive={PURPLE} emissiveIntensity={2.5} toneMapped={false} />
      </mesh>

      {/* Mic Head - Outer Crystal Shell */}
      <mesh ref={headRef} position={[0, 0.9, 0]}>
        <capsuleGeometry args={[0.35, 0.65, 32, 32]} />
        <MeshTransmissionMaterial 
          samples={16} 
          resolution={512} 
          transmission={1} 
          roughness={0.1} 
          thickness={0.5} 
          ior={1.5} 
          chromaticAberration={0.1} 
          anisotropy={0.3} 
          color="#ffffff"
        />
      </mesh>

      {/* Orbiting Rings representing sound capture */}
      <group position={[0, 0.9, 0]}>
        <mesh ref={ringRef1}>
          <torusGeometry args={[0.55, 0.008, 16, 100]} />
          <meshBasicMaterial color={CYAN} />
        </mesh>
        <mesh ref={ringRef2}>
          <torusGeometry args={[0.65, 0.008, 16, 100]} />
          <meshBasicMaterial color={BLUE} />
        </mesh>
      </group>

      {/* Mic Body - Premium Matte Metal */}
      <mesh castShadow position={[0, -0.3, 0]}>
        <cylinderGeometry args={[0.12, 0.08, 1.4, 32]} />
        <meshPhysicalMaterial color="#1a1a1a" metalness={0.8} roughness={0.6} clearcoat={0.2} />
      </mesh>

      {/* Metallic collar */}
      <mesh castShadow position={[0, 0.4, 0]}>
        <cylinderGeometry args={[0.16, 0.12, 0.1, 32]} />
        <meshPhysicalMaterial color="#cccccc" metalness={1} roughness={0.2} />
      </mesh>

      {/* Bottom glowing accent */}
      <mesh position={[0, -1.05, 0]}>
        <sphereGeometry args={[0.08, 32, 32]} />
        <meshStandardMaterial color={CYAN} emissive={CYAN} emissiveIntensity={2} toneMapped={false} />
      </mesh>
    </group>
  );
}

/** 
 * EXPANDING SOUND WAVES
 * High-end glowing rings that pulse outward to visualize audio.
 */
function SoundRings({ reduced }: { reduced: boolean }) {
  const refs = useRef<THREE.Mesh[]>([]);
  useFrame((state) => {
    if (reduced) return;
    refs.current.forEach((mesh, i) => {
      const t = (state.clock.elapsedTime * 0.25 + i / 3) % 1;
      const s = 1 + t * 4;
      mesh.scale.setScalar(s);
      
      const mat = mesh.material as THREE.MeshStandardMaterial;
      mat.opacity = 0.8 * Math.pow((1 - t), 3); // Smooth exponential fade
    });
  });
  return (
    <group rotation={[Math.PI / 2, 0, 0]} position={[0, 0.9, 0]}>
      {[0, 1, 2].map((i) => (
        <mesh
          key={i}
          ref={(el) => {
            if (el) refs.current[i] = el;
          }}
        >
          <torusGeometry args={[0.8, 0.004, 16, 128]} />
          <meshStandardMaterial 
            color={i % 2 ? BLUE : CYAN} 
            emissive={i % 2 ? BLUE : CYAN} 
            emissiveIntensity={2}
            transparent 
            opacity={0} 
            blending={THREE.AdditiveBlending}
            depthWrite={false}
          />
        </mesh>
      ))}
    </group>
  );
}

/** 
 * PREMIUM PITCH RIBBON 
 * A liquid-metal style ribbon tracing a vocal pitch path.
 */
function PitchRibbon({ z = -1.6, color = PURPLE, offset = 0 }: { z?: number; color?: string; offset?: number }) {
  const mesh = useRef<THREE.Mesh>(null);
  const geometry = useMemo(() => {
    const pts: THREE.Vector3[] = [];
    for (let i = 0; i <= 150; i++) {
      const t = i / 150;
      const x = (t - 0.5) * 8.5;
      const y = Math.sin(t * Math.PI * 2.5 + offset) * 0.6 + Math.cos(t * Math.PI * 5) * 0.25;
      const zOffset = Math.sin(t * Math.PI * 3 + offset) * 0.6;
      pts.push(new THREE.Vector3(x, y, zOffset));
    }
    return new THREE.TubeGeometry(new THREE.CatmullRomCurve3(pts), 256, 0.035, 16, false);
  }, [offset]);

  useFrame((state) => {
    if (mesh.current) {
      mesh.current.position.y = Math.sin(state.clock.elapsedTime * 0.4 + offset) * 0.15;
      mesh.current.rotation.x = Math.sin(state.clock.elapsedTime * 0.2 + offset) * 0.08;
    }
  });

  return (
    <mesh ref={mesh} geometry={geometry} position={[0, -0.2, z]} castShadow>
      <meshPhysicalMaterial 
        color={color} 
        emissive={color} 
        emissiveIntensity={0.6}
        roughness={0.1} 
        metalness={0.9} 
        clearcoat={1} 
        clearcoatRoughness={0.1}
      />
    </mesh>
  );
}

/** 
 * PREMIUM MUSIC NOTE
 * A stylized floating music note made of polished chrome and glass.
 */
function PremiumNote({ position, color }: { position: [number, number, number]; color: string }) {
  return (
    <Float speed={2} rotationIntensity={1} floatIntensity={1.5}>
      <group position={position} rotation={[0.2, 0.5, -0.2]} scale={0.65}>
        <mesh position={[-0.2, 0, 0]} rotation={[Math.PI / 2, 0, 0]} castShadow>
          <torusGeometry args={[0.25, 0.1, 32, 64]} />
          <MeshTransmissionMaterial 
            thickness={0.4} 
            roughness={0.1} 
            ior={1.5} 
            chromaticAberration={0.06} 
            transmission={0.9} 
            color={SOFT}
          />
        </mesh>
        <mesh position={[0.15, 0.55, 0]} castShadow>
          <cylinderGeometry args={[0.06, 0.06, 1.1, 32]} />
          <meshPhysicalMaterial color={color} metalness={0.9} roughness={0.1} clearcoat={1} />
        </mesh>
        <mesh position={[0.3, 1.05, 0]} rotation={[0, 0, -0.3]}>
          <boxGeometry args={[0.4, 0.08, 0.06]} />
          <meshPhysicalMaterial color={color} metalness={0.9} roughness={0.1} clearcoat={1} />
        </mesh>
      </group>
    </Float>
  );
}

/** 
 * PREMIUM CRYSTAL BLOB
 * Represents a raw vocal snippet turning into a polished gem.
 */
function PremiumCrystal({ position, color, scale = 1 }: { position: [number, number, number], color: string, scale?: number }) {
  const meshRef = useRef<THREE.Mesh>(null);
  useFrame((state) => {
    if (meshRef.current) {
      meshRef.current.rotation.x = state.clock.elapsedTime * 0.2;
      meshRef.current.rotation.y = state.clock.elapsedTime * 0.3;
    }
  });

  return (
    <Float speed={1.5} rotationIntensity={1.2} floatIntensity={2}>
      <mesh ref={meshRef} position={position} scale={scale} castShadow>
        <octahedronGeometry args={[0.6, 0]} />
        <MeshTransmissionMaterial 
          samples={16} 
          resolution={512} 
          transmission={0.95} 
          roughness={0.05} 
          thickness={0.8} 
          ior={2.2} 
          chromaticAberration={0.2} 
          color={color}
        />
      </mesh>
    </Float>
  );
}

/** 
 * PREMIUM SCORE CARD
 * A sleek frosted glass UI panel with neon data visualization.
 */
function ScoreCard({ position }: { position: [number, number, number] }) {
  return (
    <Float speed={1.5} rotationIntensity={0.3} floatIntensity={1.2}>
      <group position={position} rotation={[0.05, -0.3, 0.05]}>
        <RoundedBox args={[2.2, 1.3, 0.05]} radius={0.12} smoothness={8} castShadow>
          <MeshTransmissionMaterial 
            samples={16} 
            resolution={512} 
            transmission={0.85} 
            roughness={0.25} 
            thickness={0.1} 
            ior={1.4} 
            color="#ffffff"
          />
        </RoundedBox>
        {/* Glowing Data Bars */}
        <mesh position={[-0.2, 0.25, 0.04]}>
          <boxGeometry args={[1.4, 0.08, 0.02]} />
          <meshStandardMaterial color={PURPLE} emissive={PURPLE} emissiveIntensity={1.5} toneMapped={false} />
        </mesh>
        <mesh position={[-0.4, -0.1, 0.04]}>
          <boxGeometry args={[1.0, 0.06, 0.02]} />
          <meshStandardMaterial color={CYAN} emissive={CYAN} emissiveIntensity={1.5} toneMapped={false} />
        </mesh>
        <mesh position={[0.3, -0.1, 0.04]}>
          <boxGeometry args={[0.2, 0.06, 0.02]} />
          <meshStandardMaterial color={BLUE} emissive={BLUE} emissiveIntensity={1.5} toneMapped={false} />
        </mesh>
        <mesh position={[-0.1, -0.35, 0.04]}>
          <boxGeometry args={[1.2, 0.04, 0.02]} />
          <meshStandardMaterial color={SOFT} emissive={SOFT} emissiveIntensity={1.5} toneMapped={false} />
        </mesh>
      </group>
    </Float>
  );
}

function ParallaxRig({ enabled, children }: { enabled: boolean; children: React.ReactNode }) {
  const group = useRef<THREE.Group>(null);
  const { pointer } = useThree();
  useFrame((_, delta) => {
    if (!group.current || !enabled) return;
    const k = 1 - Math.exp(-4 * Math.min(delta, 0.05));
    group.current.rotation.y += (pointer.x * 0.25 - group.current.rotation.y) * k;
    group.current.rotation.x += (-pointer.y * 0.15 - group.current.rotation.x) * k;
  });
  return <group ref={group}>{children}</group>;
}

export default function HeroScene({ compact = false }: { compact?: boolean }) {
  const reduced = useReducedMotion();

  return (
    <Canvas
      shadows
      dpr={[1, compact ? 1.5 : 2]}
      camera={{ position: [0, 0.4, 9.2], fov: 42 }}
      gl={{ antialias: true, alpha: true, toneMapping: THREE.ACESFilmicToneMapping }}
      style={{ touchAction: "pan-y" }}
    >
      <ambientLight intensity={0.6} />
      <directionalLight
        position={[6, 8, 5]}
        intensity={2.5}
        castShadow
        shadow-mapSize={[1024, 1024]}
        shadow-bias={-0.0001}
      />
      <directionalLight position={[-5, 2, -5]} intensity={1.5} color={CYAN} />
      <spotLight position={[0, 5, 2]} intensity={3} color={PURPLE} penumbra={1} angle={0.6} castShadow />
      
      <Environment preset="city">
        <Lightformer intensity={5} color={PURPLE} position={[0, 5, -2]} scale={[20, 20, 1]} />
        <Lightformer intensity={3} color={CYAN} position={[-5, 1, -1]} rotation={[0, Math.PI / 2, 0]} scale={[20, 5, 1]} />
      </Environment>

      <ParallaxRig enabled={!compact && !reduced}>
        <group position={[compact ? 0 : 0.2, 0, 0]}>
          <Microphone compact={compact} />
          <SoundRings reduced={reduced} />
          <PitchRibbon z={-1.5} color={PURPLE} offset={0} />
          {!compact && <PitchRibbon z={-2.5} color={CYAN} offset={Math.PI} />}
          <PremiumNote position={[-2.8, 1.8, -0.4]} color={PURPLE} />
          {!compact && <PremiumNote position={[2.8, -1.2, 0.4]} color={BLUE} />}
          <PremiumCrystal position={[2.8, 1.8, -1]} color={CYAN} scale={0.9} />
          {!compact && <PremiumCrystal position={[-3.2, -1.2, -0.6]} color="#e0d4ff" scale={1.1} />}
          {!compact && <ScoreCard position={[3.2, 0.5, 0.6]} />}
          
          {/* Ambient magic dust */}
          {!reduced && (
            <Sparkles 
              count={compact ? 40 : 120} 
              scale={10} 
              size={3} 
              speed={0.4} 
              opacity={0.5} 
              color={CYAN} 
              position={[0, 0, -2]} 
            />
          )}
        </group>
      </ParallaxRig>

      {/* Ground soft shadow for premium studio feel */}
      <ContactShadows position={[0, -2.5, 0]} opacity={0.6} scale={15} blur={2.5} far={4} color="#302060" />
    </Canvas>
  );
}
