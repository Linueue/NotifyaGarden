import json
from enum import Enum

class Categories(Enum):
    SEEDS = 0
    GEARS = 1
    EGGS  = 2

class Item:
    def __init__(self, name: str, icon: str, color: int):
        self.name: str = name
        self.icon: str = icon
        self.color: int = color

    def to_json(self):
        return {"name": self.name, "icon": self.icon, "color": self.color}

class Items:
    def __init__(self, version: str):
        self.version: str = version
        self.data: dict[Categories, list[Item]] = {}

        for category in list(Categories):
            self.data[category] = []

    def add(self, category: Categories, item: Item):
        self.data[category].append(item)
    
    def serialize(self, filename: str):
        values = {}
        values["version"] = self.version
        for category, data in self.data.items():
            values[category.name.lower()] = [d.to_json() for d in data]

        with open(filename, "w", encoding="utf8") as f:
            f.write(json.dumps(values, ensure_ascii=False, indent=2))

def main():
    items = Items("1.13.0")

    items.add(Categories.SEEDS, Item("Carrot", "🥕", 0xFFFFA500))
    items.add(Categories.SEEDS, Item("Strawberry", "🍓", 0xFFDB3D21))
    items.add(Categories.SEEDS, Item("Blueberry", "🫐", 0xFF4B0082))
    items.add(Categories.SEEDS, Item("Orange Tulip", "🌷", 0xFFFFA500))
    items.add(Categories.SEEDS, Item("Tomato", "🍅", 0xFFFF6347))
    items.add(Categories.SEEDS, Item("Daffodil", "🌼", 0xFFFFFF00))
    items.add(Categories.SEEDS, Item("Watermelon", "🍉", 0xFFFC6C85))
    items.add(Categories.SEEDS, Item("Pumpkin", "🎃", 0xFFFF7518))
    items.add(Categories.SEEDS, Item("Apple", "🍎", 0xFFFF0800))
    items.add(Categories.SEEDS, Item("Bamboo", "🎋", 0xFF7BB661))
    items.add(Categories.SEEDS, Item("Coconut", "🥥", 0xFF8B4513))
    items.add(Categories.SEEDS, Item("Cactus", "🌵", 0xFF228B22))
    items.add(Categories.SEEDS, Item("Dragon Fruit", "🥝", 0xFFFF2E93))
    items.add(Categories.SEEDS, Item("Mango", "🥭", 0xFFFFC324))
    items.add(Categories.SEEDS, Item("Grape", "🍇", 0xFF6F2DA8))
    items.add(Categories.SEEDS, Item("Mushroom", "🍄", 0xFFFFDAB9))
    items.add(Categories.SEEDS, Item("Pepper", "🌶️", 0xFFFF0000))
    items.add(Categories.SEEDS, Item("Beanstalk", "🌿", 0xFF3CB371))
    items.add(Categories.SEEDS, Item("Ember Lily", "🌺", 0xFFFF4500))
    items.add(Categories.SEEDS, Item("Sugar Apple", "🍏", 0xFF7FFFD4))
    items.add(Categories.SEEDS, Item("Burning Bud", "☀️", 0xFFB22222))

    items.add(Categories.GEARS, Item("Watering Can", "🪣", 0xFFB0C4DE))
    items.add(Categories.GEARS, Item("Trowel", "🧑‍🌾", 0xFF8B4513))
    items.add(Categories.GEARS, Item("Recall Wrench", "🔧", 0xFF708090))
    items.add(Categories.GEARS, Item("Basic Sprinkler", "💧", 0xFFADD8E6))
    items.add(Categories.GEARS, Item("Advanced Sprinkler", "💦", 0xFF00BFFF))
    items.add(Categories.GEARS, Item("Godly Sprinkler", "🌧️", 0xFF1E90FF))
    items.add(Categories.GEARS, Item("Magnifying Glass", "🔍", 0xFFDAA520))
    items.add(Categories.GEARS, Item("Tanning Mirror", "🪞", 0xFFFFF5EE))
    items.add(Categories.GEARS, Item("Master Sprinkler", "🌊", 0xFF4682B4))
    items.add(Categories.GEARS, Item("Cleaning Spray", "🧴", 0xFFFFE4C4))
    items.add(Categories.GEARS, Item("Favorite Tool", "🛠️", 0xFFDAA520))
    items.add(Categories.GEARS, Item("Harvest Tool", "🌾", 0xFFDEB887))
    items.add(Categories.GEARS, Item("Friendship Pot", "🪴", 0xFF9ACD32))

    items.add(Categories.EGGS, Item("Common Egg", "🥚", 0xFFF0EAD6))
    items.add(Categories.EGGS, Item("Common Summer Egg", "🥚", 0xFFF0EAD6))
    items.add(Categories.EGGS, Item("Uncommon Egg", "🟡", 0xFFFFFF00))
    items.add(Categories.EGGS, Item("Rare Egg", "🔵", 0xFF1E90FF))
    items.add(Categories.EGGS, Item("Rare Summer Egg", "🔵", 0xFF1E90FF))
    items.add(Categories.EGGS, Item("Legendary Egg", "🟣", 0xFF8A2BE2))
    items.add(Categories.EGGS, Item("Mythical Egg", "🌟", 0xFFFFD700))
    items.add(Categories.EGGS, Item("Paradise Egg", "🌟", 0xFFFFD700))
    items.add(Categories.EGGS, Item("Bug Egg", "🐛", 0xFF556B2F))
    items.add(Categories.EGGS, Item("Bee Egg", "🐝", 0xFFFFC107))

    items.serialize("update.json")

if __name__ == "__main__":
    main()
