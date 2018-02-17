---
layout: docs
title: Potassium Principles
section: intro
---

# Potassium Principles
Potassium is built upon a set of core principles that guide its design and architecture
1. Incrementally Adoptable: Pieces of Potassium, such as the core, can be used by themselves, without forcing the user to implement all logic with Potassium
2. Modular: Potassium is broken into modules across different uses, so that it can be used in many contexts. For example, FIRST Robotics specific logic is broken out into a separate module so that Potassium can be used on non-FRC platforms
3. Flexible: Potassium provides many locations where users can swap in custom logic for their use cases, without being forced into an all-or-none decision
4. Testable: Each component of Potassium can be tested independently by itself