package no.nav.foreldrepenger.tilganger.integrasjoner.redis;

/**
 * Denne klassen definerer de forskjellige 15 databaser som finnes i Redis. Om man cacher forskjellige områder i egne databaser så blir det enklere
 * å evicte hele cachen uten å påvirke andre cache.
 */
public enum RedisDatabase {
        ZERO,
        ONE,
        TWO,
        THREE,
        FOUR,
        FIVE,
        SIX,
        SEVEN,
        EIGHT,
        NINE,
        TEN,
        ELEVEN,
        TWELVE,
        THIRTEEN,
        FOURTEEN,
        FIFTEEN;
}
