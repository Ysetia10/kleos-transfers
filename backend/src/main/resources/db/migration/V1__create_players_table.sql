CREATE TABLE players (
    id UUID PRIMARY KEY,
    full_name VARCHAR(100) NOT NULL,
    date_of_birth DATE NOT NULL,
    nationality VARCHAR(3) NOT NULL,
    height_cm INTEGER NOT NULL CHECK (height_cm BETWEEN 140 AND 230),
    preferred_foot VARCHAR(5) NOT NULL CHECK (preferred_foot IN ('LEFT', 'RIGHT', 'BOTH')),
    primary_position VARCHAR(3) NOT NULL CHECK (primary_position IN (
        'GK', 'RB', 'CB', 'LB', 'RWB', 'LWB', 'CDM', 'CM', 'CAM', 'RM', 'LM', 'RW', 'LW', 'CF', 'ST'
    )),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL
);
