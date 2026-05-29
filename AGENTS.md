# ToneShift Project Specification

## Overview

ToneShift is a modern web application for converting Vietnamese musical notation (cảm âm) between different musical keys/tones.

Example use case:

* User has notation in tone D (Rê)
* User needs to play in tone C (Đô)
* System automatically transposes all notes correctly while preserving lyrics and formatting

The application targets:

* Sáo trúc players
* Harmonica players
* Guitar learners
* Vietnamese traditional music community
* Bolero / cải lương musicians

---

# Core Features

## 1. Tone Converter

Main feature of the project.

### Input

User pastes notation text.

Example:

```txt
Re Re la Sol La Si
```

### Output

System converts notes to target tone.

Example:

```txt
Do Do sol Fa Sol La
```

### Requirements

* Preserve original formatting
* Preserve lyrics
* Preserve uppercase/lowercase
* Support combined notes:

    * LaRe3
    * SolFa
    * ReDo
* Support octave numbers:

    * Do3
    * Re2
* Realtime conversion

---

## 2. Tone Mapping Engine

### Example Mapping

#### D → C

```txt
Re  -> Do
Mi  -> Re
Fa  -> Mib
Sol -> Fa
La  -> Sol
Si  -> La
Do  -> Sib
```

### Requirements

* Support all 12 musical keys
* Support sharp/flat notation
* Dynamic interval calculation
* Accurate transposition

---

## 3. Smart Note Detection

System should automatically detect notes inside mixed text.

Example:

```txt
Từ là từ phu tướng
Re Re la Sol La Si
```

Only convert:

```txt
Re Re la Sol La Si
```

Do NOT modify lyrics.

---

## 4. Syntax Highlighting

### Note Colors

* Notes: blue/purple
* Lyrics: white/zinc
* Tone labels: yellow

### Example

```txt
Re Re la Sol
```

Should visually highlight note tokens.

---

## 5. Auto Tone Detection

System suggests original key automatically.

Example:

Input:

```txt
Re Sol La Si
```

Output:

```txt
Detected Tone: D Major
```

---

## 6. Copy / Export

Users can:

* Copy converted notation
* Download TXT
* Export PDF
* Export image

---

## 7. History

Save conversion history locally.

Each item:

* title
* original tone
* target tone
* timestamp
* preview

---

## 8. Community Library

Users can:

* Upload songs
* Browse public notation
* Search by tone
* Filter by genre

Genres:

* Bolero
* Cải lương
* Sáo trúc
* Nhạc trẻ
* Guitar

---

# UI/UX Design

## Design Style

Modern dark UI inspired by:

* Spotify
* Linear
* Notion
* Vercel

---

# Color Palette

## Main Colors

```txt
Background: #09090B
Card: #18181B
Primary: #2563EB
Accent: #60A5FA
Text: #FAFAFA
Border: rgba(255,255,255,0.08)
```

---

# Typography

Recommended fonts:

* Geist
* Inter

---

# Layout Structure

## Header

### Left

* Logo
* App name

### Center

Navigation:

* Converter
* Library
* Tools
* Community
* Docs

### Right

* Search
* Theme toggle
* Login button
* CTA button

---

# Homepage Sections

## Hero Section

### Title

```txt
Convert Vietnamese Music Notation Instantly
```

### Subtitle

```txt
Transpose cảm âm between musical keys automatically.
```

### CTA

```txt
Start Converting
```

---

## Main Converter Section

### Two-column Layout

#### Left Panel

Original notation textarea.

#### Right Panel

Converted result preview.

---

# Suggested Components

Use shadcn/ui components:

* Button
* Card
* Textarea
* Select
* Tabs
* Dialog
* Sheet
* Tooltip
* Toast
* Accordion
* Dropdown Menu

---

# Suggested Tech Stack

## Frontend

* Next.js
* TypeScript
* Tailwind CSS
* shadcn/ui
* Framer Motion
* Lucide Icons

---

# Folder Structure

```txt
src/
 ├── app/
 ├── components/
 │    ├── layout/
 │    ├── converter/
 │    ├── ui/
 │    └── music/
 ├── lib/
 │    ├── transpose/
 │    ├── parser/
 │    └── utils/
 ├── hooks/
 ├── services/
 ├── store/
 └── types/
```

---

# Core Algorithms

## Tokenization

Detect:

```txt
LaRe3
SolFa
Do
```

into separate note tokens.

---

## Transposition Algorithm

### Steps

1. Detect original tone
2. Calculate interval
3. Parse notation
4. Replace notes
5. Preserve formatting
6. Rebuild final output

---

# Future Features

## AI Features

* AI notation cleanup
* Auto formatting
* AI playable key suggestion
* Audio generation
* Voice input

---

# Mobile App

Future mobile version:

* Expo
* Offline conversion
* Saved songs
* Audio playback

---

# SEO Keywords

* đổi tone cảm âm
* chuyển tone nhạc
* cảm âm sáo trúc
* transpose Vietnamese music
* cảm âm cải lương
* đổi tone guitar

---

# Project Goals

Build the best Vietnamese music notation transposer platform with:

* Modern UI
* Fast conversion
* AI assistance
* Community sharing
* Multi-platform support
