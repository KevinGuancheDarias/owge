//! Byte-exact port of `java.util.Random`.
//!
//! `java.util.Random` is **contractually specified** by the Java Language /
//! library spec and is therefore stable across every JVM implementation and
//! version. This module reproduces that contract bit-for-bit so the Rust attack
//! engine can consume the *exact same* random sequence as the Java backend for a
//! given seed, making combat reproducible across the two backends.
//!
//! See `rust-backend/pending_migration/ATTACK_PARITY_PLAN.md`, "Part 1 —
//! JavaRandom". The golden-vector unit tests at the bottom of this file assert
//! the output against values captured from a real JDK 21.
//!
//! ## Notes on the Java side
//!
//! - `Math.random()` is defined as a single process-wide
//!   `Random.nextDouble()` on a lazily-initialised shared `Random`. Under
//!   deterministic combat mode the caller does **not** rely on that shared
//!   instance — it passes an explicit seeded [`JavaRandom`] and calls
//!   [`JavaRandom::next_double`].
//! - `Collections.shuffle(list)` (no explicit `Random`) likewise uses a shared
//!   `Random`. The deterministic path instead calls
//!   [`JavaRandom::shuffle`] with an explicit seeded instance so the shuffle
//!   order is reproducible.

const MULTIPLIER: i64 = 0x5DEECE66D;
const ADDEND: i64 = 0xB;
/// 48-bit mask: `(1 << 48) - 1`.
const MASK: i64 = (1 << 48) - 1;

/// A byte-exact reimplementation of `java.util.Random`.
///
/// The internal state is a 48-bit seed advanced by a linear congruential
/// generator, identical to the JDK. Construct with [`JavaRandom::new`] (which
/// applies the same constructor scramble as `new java.util.Random(seed)`).
#[derive(Debug, Clone)]
pub struct JavaRandom {
    /// 48-bit LCG state. Always kept within `0..=MASK`.
    seed: i64,
}

impl JavaRandom {
    /// Mirrors `new java.util.Random(seed)`: scrambles the supplied seed with the
    /// LCG multiplier and masks it to 48 bits.
    #[must_use]
    pub fn new(seed: i64) -> Self {
        Self {
            seed: (seed ^ MULTIPLIER) & MASK,
        }
    }

    /// Mirrors `protected int next(int bits)`.
    ///
    /// Advances the 48-bit LCG state and returns the top `bits` bits as a signed
    /// 32-bit integer. The shift is arithmetic (sign-propagating), matching
    /// Java's `(int)(seed >>> (48 - bits))` for the values `next` is used with.
    fn next(&mut self, bits: u8) -> i32 {
        self.seed = self
            .seed
            .wrapping_mul(MULTIPLIER)
            .wrapping_add(ADDEND)
            & MASK;
        // Java: (int)(seed >>> (48 - bits)). seed is non-negative (masked to 48
        // bits) so the logical and arithmetic shifts coincide; the resulting
        // top bit becomes the sign bit of the i32, reproducing Java's int cast.
        (self.seed >> (48 - bits as i64)) as i32
    }

    /// Mirrors `int nextInt(int bound)`. `bound` must be strictly positive.
    ///
    /// # Panics
    ///
    /// Panics if `bound <= 0`, matching Java throwing
    /// `IllegalArgumentException`.
    #[must_use]
    pub fn next_int_bound(&mut self, bound: i32) -> i32 {
        assert!(bound > 0, "bound must be positive");

        // Power-of-two fast path: (bound & -bound) == bound.
        if (bound & bound.wrapping_neg()) == bound {
            return ((bound as i64).wrapping_mul(self.next(31) as i64) >> 31) as i32;
        }

        // Rejection loop. The `bits - val + (bound - 1) >= 0` test is the exact
        // JDK overflow guard; it must use wrapping i32 arithmetic so it matches
        // Java's `int` overflow semantics.
        loop {
            let bits = self.next(31);
            let val = bits % bound;
            if bits
                .wrapping_sub(val)
                .wrapping_add(bound - 1)
                >= 0
            {
                return val;
            }
        }
    }

    /// Mirrors `double nextDouble()`.
    ///
    /// Combines a 26-bit and a 27-bit draw into a 53-bit mantissa over `2^53`.
    #[must_use]
    pub fn next_double(&mut self) -> f64 {
        (((self.next(26) as i64) << 27) + self.next(27) as i64) as f64 / (1i64 << 53) as f64
    }

    /// Mirrors `java.util.Collections.shuffle(list, this)` for a slice.
    ///
    /// Performs the Fisher–Yates swap in the exact JDK order: for `i` from `size`
    /// down to `2`, swap element `i - 1` with element `next_int_bound(i)`.
    pub fn shuffle<T>(&mut self, list: &mut [T]) {
        let size = list.len();
        for i in (2..=size).rev() {
            let j = self.next_int_bound(i as i32) as usize;
            list.swap(i - 1, j);
        }
    }
}

#[cfg(test)]
mod tests {
    use super::*;

    // Golden vectors captured from a real JDK 21 (`maven:3.9-eclipse-temurin-21`)
    // via the throwaway `Gen.java` described in ATTACK_PARITY_PLAN.md, Part 1.

    fn next_int_vec(seed: i64, bound: i32) -> Vec<i32> {
        let mut r = JavaRandom::new(seed);
        (0..20).map(|_| r.next_int_bound(bound)).collect()
    }

    fn next_double_bits(seed: i64) -> Vec<u64> {
        let mut r = JavaRandom::new(seed);
        (0..20).map(|_| r.next_double().to_bits()).collect()
    }

    fn shuffle_vec(seed: i64) -> Vec<i32> {
        let mut r = JavaRandom::new(seed);
        let mut list: Vec<i32> = (0..12).collect();
        r.shuffle(&mut list);
        list
    }

    #[test]
    fn next_int_matches_jdk() {
        // seed=0
        assert_eq!(
            next_int_vec(0, 16),
            vec![11, 13, 3, 9, 10, 4, 8, 1, 9, 12, 5, 4, 6, 9, 15, 15, 14, 0, 15, 2]
        );
        assert_eq!(
            next_int_vec(0, 10),
            vec![0, 8, 9, 7, 5, 3, 1, 1, 9, 4, 7, 7, 3, 2, 5, 4, 4, 5, 1, 0]
        );
        assert_eq!(
            next_int_vec(0, 7),
            vec![5, 2, 4, 2, 4, 0, 2, 1, 6, 2, 1, 2, 0, 2, 4, 0, 3, 0, 6, 0]
        );
        assert_eq!(
            next_int_vec(0, 1000),
            vec![
                360, 948, 29, 447, 515, 53, 491, 761, 719, 854, 77, 677, 473, 262, 95, 844, 84,
                875, 241, 320
            ]
        );

        // seed=1
        assert_eq!(
            next_int_vec(1, 16),
            vec![11, 1, 6, 6, 3, 0, 5, 10, 15, 11, 0, 2, 15, 2, 15, 8, 15, 14, 14, 7]
        );
        assert_eq!(
            next_int_vec(1, 10),
            vec![5, 8, 7, 3, 4, 4, 4, 6, 8, 8, 9, 3, 7, 3, 2, 4, 2, 2, 6, 9]
        );
        assert_eq!(
            next_int_vec(1, 7),
            vec![4, 4, 1, 0, 6, 6, 0, 1, 3, 6, 5, 5, 5, 5, 2, 2, 0, 0, 0, 5]
        );
        assert_eq!(
            next_int_vec(1, 1000),
            vec![
                985, 588, 847, 313, 254, 904, 434, 606, 978, 748, 569, 473, 317, 263, 562, 234,
                592, 262, 596, 189
            ]
        );

        // seed=42
        assert_eq!(
            next_int_vec(42, 16),
            vec![11, 0, 10, 0, 4, 15, 4, 11, 10, 1, 14, 7, 5, 6, 4, 11, 7, 12, 12, 15]
        );
        assert_eq!(
            next_int_vec(42, 10),
            vec![0, 3, 8, 4, 0, 5, 5, 8, 9, 3, 2, 2, 6, 2, 6, 2, 6, 0, 3, 9]
        );
        assert_eq!(
            next_int_vec(42, 7),
            vec![1, 5, 6, 3, 5, 4, 1, 3, 6, 3, 3, 4, 0, 0, 1, 3, 0, 5, 0, 2]
        );
        assert_eq!(
            next_int_vec(42, 1000),
            vec![
                130, 763, 248, 884, 970, 525, 505, 918, 519, 93, 182, 502, 276, 292, 476, 32, 456,
                170, 743, 209
            ]
        );

        // seed=-1
        assert_eq!(
            next_int_vec(-1, 16),
            vec![4, 7, 0, 8, 10, 9, 6, 6, 13, 0, 12, 4, 0, 1, 1, 11, 11, 14, 8, 11]
        );
        assert_eq!(
            next_int_vec(-1, 10),
            vec![3, 5, 9, 9, 4, 8, 7, 8, 5, 1, 8, 2, 7, 6, 2, 0, 9, 2, 6, 5]
        );
        assert_eq!(
            next_int_vec(-1, 7),
            vec![3, 6, 4, 6, 6, 4, 6, 2, 3, 6, 5, 5, 2, 0, 6, 3, 2, 2, 1, 0]
        );
        assert_eq!(
            next_int_vec(-1, 1000),
            vec![
                913, 225, 579, 439, 604, 438, 477, 478, 765, 731, 808, 512, 317, 196, 342, 250, 69,
                972, 686, 655
            ]
        );

        // seed=123456789
        assert_eq!(
            next_int_vec(123456789, 16),
            vec![10, 12, 7, 4, 6, 3, 14, 6, 11, 6, 5, 7, 7, 0, 7, 7, 1, 1, 3, 10]
        );
        assert_eq!(
            next_int_vec(123456789, 10),
            vec![5, 0, 3, 4, 0, 4, 3, 7, 7, 7, 1, 1, 4, 2, 4, 1, 0, 9, 0, 9]
        );
        assert_eq!(
            next_int_vec(123456789, 7),
            vec![1, 0, 6, 3, 4, 0, 0, 1, 6, 1, 5, 6, 4, 1, 5, 6, 0, 3, 0, 0]
        );
        assert_eq!(
            next_int_vec(123456789, 1000),
            vec![
                965, 600, 483, 344, 290, 554, 533, 807, 297, 887, 441, 961, 664, 982, 224, 261,
                360, 789, 530, 279
            ]
        );
    }

    #[test]
    fn next_double_matches_jdk() {
        // Compared by exact IEEE-754 bit pattern (doubleToLongBits).
        assert_eq!(
            next_double_bits(0),
            vec![
                0x3fe7_6416_8ea6_ca89,
                0x3fce_c9e5_b367_2e14,
                0x3fe4_65b9_3a78_ef81,
                0x3fe1_9d2e_10ef_a128,
                0x3fe3_1f17_4640_953b,
                0x3fd5_5373_440b_5f04,
                0x3fd8_a6f0_89ce_fe94,
                0x3fef_83d2_67dc_d07a,
                0x3fec_2243_602f_4588,
                0x3fee_1eb6_9968_6687,
                0x3fd1_98d8_8501_c926,
                0x3fc0_7fb3_abc6_751c,
                0x3fc2_c3d7_d690_caa4,
                0x3f97_cbbc_005a_e0a0,
                0x3fe1_7ee4_6012_e47a,
                0x3fee_dd13_8c80_ebc3,
                0x3fba_bfe6_d02f_3cb8,
                0x3fe4_0132_f249_8873,
                0x3fda_4a7c_203d_242c,
                0x3fe8_d78c_dfd0_46f4,
            ]
        );
        assert_eq!(
            next_double_bits(1),
            vec![
                0x3fe7_635a_a8cd_c4e6,
                0x3fda_3ec3_9684_df98,
                0x3fca_9666_6128_d71c,
                0x3fd5_4b3c_7a8a_b85c,
                0x3fee_f7db_3daf_9843,
                0x3f79_0e54_9c66_e000,
                0x3fee_d6ab_7146_d0db,
                0x3fee_1360_946e_bd77,
                0x3fee_4f6b_b749_a067,
                0x3fed_fc93_b3e5_8e11,
                0x3fd9_6b4d_ee9f_9788,
                0x3fd6_3dbc_428c_a7a2,
                0x3fd2_d1d4_95e7_6f38,
                0x3fe0_351d_26ea_6b93,
                0x3fbd_b004_e283_3af0,
                0x3fe8_a83a_dcaa_b56c,
                0x3fe5_1dd7_5056_a083,
                0x3fc4_1048_385f_a39c,
                0x3fd8_3476_59fb_caa0,
                0x3fc1_e3be_5c22_ec14,
            ]
        );
        assert_eq!(
            next_double_bits(42),
            vec![
                0x3fe7_4833_a06f_f457,
                0x3fe5_dcf7_7862_2e01,
                0x3fd3_c20f_3f12_bbb4,
                0x3fd1_bba7_6b52_c856,
                0x3fe5_4c2d_50bb_0864,
                0x3fec_e86c_f39c_2cbe,
                0x3fd7_9a23_a61b_35c8,
                0x3fd1_a5db_3b0b_fbe2,
                0x3fdd_ac80_0c31_8574,
                0x3fe9_0d88_07fc_450f,
                0x3fed_6b22_1937_35e3,
                0x3fdb_ef77_d709_6b88,
                0x3fe7_ff3b_3f71_eec2,
                0x3fd8_bd82_fcc5_c830,
                0x3fc6_b456_84d1_48f0,
                0x3fe3_04ea_1ab4_daca,
                0x3fca_d9a9_e805_6ae0,
                0x3fea_6e4f_faeb_d9a3,
                0x3fc6_0b3c_c513_b2bc,
                0x3fe2_cc34_8231_8166,
            ]
        );
        assert_eq!(
            next_double_bits(-1),
            vec![
                0x3fd1_365b_2708_722c,
                0x3f89_2101_1897_ff00,
                0x3fe5_2fcb_ccce_6dda,
                0x3fda_a6ec_e663_7c50,
                0x3fea_6ff3_7873_c9c7,
                0x3fe9_cb0e_a27a_675a,
                0x3fad_1380_08e9_7f40,
                0x3fb5_1643_ec2a_8d08,
                0x3fe7_2242_b738_8ef3,
                0x3fe1_e5d0_05da_2b68,
                0x3fe3_2ce0_7acf_4463,
                0x3fed_c492_73a2_4dfe,
                0x3fe3_e511_e2af_173d,
                0x3fcd_c706_4017_e6c8,
                0x3fc6_885e_e4d7_ec20,
                0x3fe9_f688_258d_a2a7,
                0x3fce_8e7a_c360_4038,
                0x3fbb_b8ad_2c93_3b88,
                0x3fe3_14d4_f2ce_96ad,
                0x3fee_3c90_1a4d_3caa,
            ]
        );
        assert_eq!(
            next_double_bits(123456789),
            vec![
                0x3fe5_3fcc_d61b_45a8,
                0x3fdd_3eb2_b413_f22a,
                0x3fd8_fe0e_e377_2df2,
                0x3fec_9640_3367_674c,
                0x3fe7_fd10_9b72_b1f1,
                0x3fd7_01c4_27d8_6cb8,
                0x3fde_6c80_c041_7aae,
                0x3fdf_2e41_77a6_fba2,
                0x3fbd_5d12_0789_9ff0,
                0x3fcf_d9a6_1444_0be8,
                0x3fd1_8f27_a802_291a,
                0x3fe2_0be2_f344_ada3,
                0x3fc8_876f_1e54_c2ec,
                0x3fdf_abb0_b98a_ad28,
                0x3feb_79ce_e225_e11d,
                0x3fe5_611e_53df_89f7,
                0x3fe3_0002_6c00_94a2,
                0x3fcd_a148_4f81_66f4,
                0x3fec_1271_68fc_04e9,
                0x3fe0_667e_bef5_fe1d,
            ]
        );
    }

    #[test]
    fn shuffle_matches_jdk() {
        assert_eq!(shuffle_vec(0), vec![4, 10, 3, 8, 1, 7, 11, 5, 2, 9, 6, 0]);
        assert_eq!(shuffle_vec(1), vec![5, 0, 4, 3, 11, 2, 8, 1, 10, 7, 6, 9]);
        assert_eq!(shuffle_vec(42), vec![0, 1, 6, 7, 3, 5, 10, 11, 9, 8, 4, 2]);
        assert_eq!(shuffle_vec(-1), vec![0, 2, 1, 6, 10, 3, 4, 11, 8, 9, 7, 5]);
        assert_eq!(
            shuffle_vec(123456789),
            vec![11, 10, 5, 8, 2, 7, 0, 9, 4, 3, 6, 1]
        );
    }
}
