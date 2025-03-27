# AutoOxidize

AutoOxidize simplifies the process of obtaining copper blocks at different oxidation levels. Instead of manually scraping blocks with an axe, this mod automatically places, scrapes, and mines copper blocks to achieve your desired oxidation stage.

# Mod Preview

![ezgif com-optimize](https://github.com/user-attachments/assets/b9b0907d-c2ec-4041-9c32-cf51be892500)


## Features

- Automatically create copper blocks at any oxidation stage (NORMAL, EXPOSED, WEATHERED, OXIDIZED)
- Support for both regular and waxed copper blocks
- Customizable timing parameters to optimize performance on different servers
- Simple command interface with helpful guidance

## Usage

### Basic Commands

- `/copper <stage>` - Start the process with regular copper block and default delays
- `/copper <stage> <waxed>` - Specify if you want to use waxed copper blocks (true/false)
- `/copper <stage> <waxed> <placeDelay> <scrapeDelay> <mineDelay> <cycleDelay>` - Start with custom delay settings
- `/copper stop` - Stop the current process
- `/copper help` - Display help information

### Parameters

- `<stage>` - The desired oxidation stage: NORMAL, EXPOSED, WEATHERED, OXIDIZED
- `<waxed>` - Whether to use waxed copper blocks: true/false
- `<placeDelay>` - Delay between placing blocks (2-100 ticks)
- `<scrapeDelay>` - Delay between axe scraping actions (2-100 ticks)
- `<mineDelay>` - Delay between mining ticks (1-20 ticks)
- `<cycleDelay>` - Delay between full cycles (2-200 ticks)

### Example

To create a regular weathered copper block with default timing:
```
/copper WEATHERED
```

To create a waxed exposed copper block with custom timing:
```
/copper EXPOSED true 20 10 2 40
```

## How It Works

1. The mod places an oxidized copper block in front of the player
2. It automatically scrapes the block with an axe to reach the desired oxidation stage
3. Once the target stage is reached, it mines the block for you to collect
4. The process repeats until stopped

## Required Tools

Make sure you have the following items in your inventory:
- Oxidized copper blocks (or waxed oxidized copper blocks)
- An axe (any type)
- A pickaxe (any type)

## License

This project is licensed under the MIT License - see the LICENSE file for details.

## Contributing

Contributions are welcome! Feel free to open issues or submit pull requests on GitHub.
