# AI Werewolf Game

> [中文文档](README.md) | English

## Overview

AI Werewolf is an AI-driven Werewolf (Mafia-like) game system that uses Large Language Models (LLMs) to power AI players. The project provides a complete game framework including a judge system, AI player prompt templates, role strategies, API management interface, voice system integration, and game UI display rules.

## Core Features

- **Judge System**: Strictly follows Werewolf game rules to host games, control flow, and process actions
- **AI Players**: LLM-prompt-driven AI players with different personalities and strategies
- **Role Strategies**: Detailed strategic guidance for different roles (Werewolf, Seer, Witch, Villager, etc.)
- **Game Configuration**: Supports 9-player and 12-player games with multiple board configurations
- **API Management**: Flexible API configuration and allocation system
- **Voice System**: Integrated multiple TTS engines with personalized voice support
- **UI Display**: Complete game interface display rules and interaction logic
- **Game Records**: Full game process recording with battle report generation and analysis

## Project Structure

```
AI-WerewolfGame/
├── frontend/                  # Vue 3 frontend (Vite + Element Plus)
│   ├── src/
│   │   ├── views/             # Page components
│   │   │   ├── Home.vue       # Landing page
│   │   │   ├── Login.vue      # Login
│   │   │   ├── Register.vue   # Registration
│   │   │   ├── ApiConfig.vue  # AI Player management (CRUD)
│   │   │   ├── game/
│   │   │   │   ├── CreateRoom.vue  # Room creation
│   │   │   │   ├── RoomList.vue    # Room listing
│   │   │   │   ├── RoomDetail.vue  # Room details
│   │   │   │   └── GamePlay.vue    # Main game board
│   │   ├── components/        # Shared components
│   │   ├── stores/            # Pinia state management
│   │   ├── router/            # Vue Router config
│   │   ├── i18n/              # Internationalization (zh-CN, en-US)
│   │   └── composables/       # Composables (useTypewriter, etc.)
│   └── package.json
├── backend/                   # Spring Boot backend
│   ├── src/main/java/com/werewolf/game/
│   │   ├── controller/        # REST API controllers
│   │   ├── service/           # Business logic
│   │   ├── entity/            # JPA entities
│   │   └── mapper/            # MyBatis-Plus mappers
│   └── pom.xml
├── configs/                   # Game board configurations
│   ├── 9p_standard.json
│   ├── 12p_wolfking_guard.json
│   └── 12p_miracle_merchant.json
├── templates/                 # Prompt templates
│   ├── judge_prompt.txt
│   ├── player_prompt.txt
│   └── roles/                 # Role strategy templates
└── README.md                  # Project documentation (Chinese)
```

## Quick Start

### Prerequisites

- Node.js 18+ and npm
- Java 8+ and Maven
- MySQL 8.0+
- Redis (optional)

### Backend Setup

1. Configure database connection in `backend/src/main/resources/application.yml`
2. Run the Spring Boot application:
   ```bash
   cd backend
   mvn spring-boot:run
   ```
3. The server starts on port 8081. Database tables are created automatically.

### Frontend Setup

1. Install dependencies:
   ```bash
   cd frontend
   npm install
   ```
2. Start the development server:
   ```bash
   npm run dev
   ```
3. Access the application at `http://localhost:5173`

### Configuration

1. **AI API Configuration**: Go to "AI Player Management" to configure API keys, model types, and provider settings. Use the "Quick Setup" feature for popular providers (OpenAI, DeepSeek, Anthropic, ModelScope).
2. **Create AI Players**: Set up AI players with different personalities and strategies.
3. **Create a Room**: Choose a game board and player count.
4. **Add AI Players**: Click empty positions in the game board to add AI players.
5. **Start the Game**: Click "Start Game" to begin.

## Game Boards

| Board | Players | Role Distribution |
|-------|---------|-------------------|
| Standard | 9 | 3 Wolves, 3 Villagers, 1 Seer, 1 Witch, 1 Hunter |
| Wolf King & Guard | 12 | 3 Wolves, 1 Wolf King, 4 Villagers, 1 Seer, 1 Witch, 1 Hunter, 1 Guard |
| Miracle Merchant | 12 | 3 Wolves, 1 Wolf King, 4 Villagers, 1 Seer, 1 Witch, 1 Guard, 1 Miracle Merchant |
| Wolf Beauty & Knight | 12 | 3 Wolves, 1 Wolf Beauty, 4 Villagers, 1 Seer, 1 Witch, 1 Knight, 1 Guard |
| White Wolf & Knight | 12 | 3 Wolves, 1 White Wolf, 4 Villagers, 1 Seer, 1 Witch, 1 Knight, 1 Guard |
| Gargoyle & Gravedigger | 12 | 3 Wolves, 1 Gargoyle, 4 Villagers, 1 Seer, 1 Witch, 1 Gravedigger, 1 Hunter |

## AI Player Configuration

### Personality Types
- **Cautious**: Logical and careful with wording
- **Aggressive**: Direct attacks with strong language
- **Logical**: Clear reasoning and deep analysis
- **Intuitive**: Gut-feeling based decisions
- **Performative**: Vivid and persuasive speech
- **Silent**: Concise and sparse speech

### Strategy Tendencies
- Conservative, Aggressive, Balanced, Random, Learning

### Provider Support
- **OpenAI**: GPT-4o, GPT-4o-mini, GPT-3.5-turbo
- **DeepSeek**: DeepSeek V3, DeepSeek R1 (with reasoning)
- **Anthropic**: Claude Sonnet 4, Claude 3.5 Sonnet, Claude 3 Haiku
- **ModelScope**: Qwen series, etc.

## Game Flow

### Night Phase
1. Judge announces "Night falls"
2. Roles act in order: Wolves → Seer → Miracle Merchant → Witch → Guard
3. All actions are recorded

### Day Phase
1. Judge announces "Day breaks"
2. Night results are revealed
3. Sheriff election (Day 1 only)
4. Speech phase
5. Voting phase
6. Proceed to next night

## Win Conditions

- **Good Team**: All wolves eliminated
- **Wolf Team**: Side-kill rule — all Villagers eliminated or all God-roles eliminated

## Game Terminology

- **Check-Kill (查杀)**: The Seer checks a player as Werewolf and reveals it
- **Gold Water (金水)**: A player verified as good by the Seer
- **Silver Water (银水)**: A player saved by the Witch's antidote
- **Sheriff Run (上警)**: Participating in Sheriff election
- **Badge Flow (警徽流)**: The Seer's arrangement for the Sheriff badge
- **Self-Destruct (自爆)**: A werewolf reveals themselves and flips their card to end the day phase
- **Vote Rally (归票)**: The last speaker calls for a vote against a specific player
- **Wolf Rush (冲票)**: Werewolves coordinate to vote out a specific player

## Features

### 🌐 Bilingual Support
Seamless Chinese-English switching with a language toggle button in the header.

### 💬 AI Dialogue Display
- **Typewriter Effect**: Progressive text reveal at configurable speeds (Slow/Normal/Fast)
- **Thinking Process**: Optional toggle to view AI reasoning content (for models that support it, like DeepSeek R1)
- **Skip Button**: Instantly display the full text

### ⚙️ Simplified API Configuration
- **Quick Setup**: One-click presets for OpenAI, DeepSeek, Anthropic, ModelScope
- **Global Defaults**: Set default API keys and URLs inherited by new AI players
- **Connection Test**: Verify API connectivity before saving

## Tech Stack

- **Frontend**: Vue 3, Vite, Element Plus, Pinia, Vue Router, Axios
- **Backend**: Spring Boot 2.5, MyBatis-Plus, MySQL, Redis, JWT
- **AI**: OpenAI API, DeepSeek API, Anthropic API, ModelScope API

## Development

### Prompt System
- **Judge Prompts**: Located in `templates/judge_prompt.txt`, customizable rules and flow
- **Player Prompts**: Located in `templates/player_prompt.txt`, adjustable personality and strategy
- **Role Strategies**: Located in `templates/roles/`, extensible role strategies

### API Integration
- Supports multiple LLM API providers
- Configurable via the web UI or `src/api/api_config.json`
- Custom request/response format support

### Voice System
- Multiple TTS engine support
- Configurable via the web UI or `src/voice/voice_config.json`
- Emotion analysis and personalized voices

## Important Notes

1. Keep API keys secure
2. Adjust timeout settings based on network conditions
3. Large games may take considerable time to complete
4. Recommended to run on capable hardware

## License

This project is licensed under the MIT License.

## Contributing

Issues and Pull Requests are welcome to help improve this AI Werewolf project.

---

## Version History

- v1.0.0: Initial release with basic game functionality

## Contact

For questions or suggestions, please contact the project maintainer.
